"""Demo assessment grader. Run only on synthetic code in a disposable, restricted environment.
Usage: python grade_submission.py STUDENT_DIRECTORY REFERENCE_DIRECTORY TESTER_DIRECTORY
Output: one TSV row per question: question, earned, maximum, passed, total, status.
"""
import ast, copy, importlib, os, pathlib, subprocess, sys, contextlib, io, tempfile, shutil

QUESTIONS = [('q1a','compute_parking',3),('q1b','compute_rebate',5),('q2a','get_digits_of_even_index',3),('q2b','count_non_digits',5),('q3a','is_partially_compliant',1),('q3b','is_fully_compliant',2),('q3c','make_tables_fully_compliant',2),('q4','schedule_delivery',4)]

def get_cases(testfile, function):
    tree=ast.parse(testfile.read_text(encoding='utf-8-sig'))
    env={}; cases=[]
    def value(node):
        if isinstance(node, ast.Name): return copy.deepcopy(env[node.id])
        return ast.literal_eval(node)
    for node in tree.body:
        if isinstance(node,ast.Assign) and len(node.targets)==1 and isinstance(node.targets[0],ast.Name):
            try: env[node.targets[0].id]=value(node.value)
            except (ValueError,TypeError,KeyError,AttributeError): pass
        expr = node.value if isinstance(node,(ast.Assign,ast.Expr)) else None
        if isinstance(expr,ast.Call):
            called=expr.func.id if isinstance(expr.func,ast.Name) else None
            if called==function or (function=='make_tables_fully_compliant' and called=='test_part_c'):
                try: cases.append([value(arg) for arg in expr.args])
                except (ValueError,TypeError,KeyError,AttributeError): pass
    return cases

# Worker runs as a separate process for every case, preventing a single infinite loop
# from stopping the rest of the batch. Parent enforces the timeout.
WORKER='''import sys, pathlib, importlib, ast, contextlib, io, copy
root=pathlib.Path(sys.argv[1]); question=sys.argv[2]; function=sys.argv[3]; args=ast.literal_eval(sys.argv[4])
sys.path.insert(0,str(root))
with contextlib.redirect_stdout(io.StringIO()):
    module=importlib.import_module(question)
    result=getattr(module,function)(*copy.deepcopy(args))
print(repr(result))
'''

def execute(folder,question,function,args,timeout=3):
    try:
        p=subprocess.run([sys.executable,'-I','-c',WORKER,str(folder.resolve()),question,function,repr(args)],capture_output=True,text=True,timeout=timeout, cwd=str(folder),env={**os.environ,'PYTHONDONTWRITEBYTECODE':'1'})
        if p.returncode: return ('ERROR',p.stderr.strip()[-200:])
        return ('OK',ast.literal_eval(p.stdout.strip()))
    except subprocess.TimeoutExpired: return ('TIMEOUT','Exceeded 3 seconds')
    except (SyntaxError,ValueError,OverflowError) as e: return ('ERROR',str(e))

def main():
    student,reference,testers=map(lambda x:pathlib.Path(x).resolve(),sys.argv[1:4])
    total=0
    for question,function,maximum in QUESTIONS:
        cases=get_cases(testers/(question+'_test.py'),function)
        passed=0; issues=[]
        sys.path.insert(0,str(reference))
        try:
            with contextlib.redirect_stdout(io.StringIO()):
                module=importlib.import_module(question)
            trusted_function=getattr(module,function)
        except Exception as exc:
            print('REFERENCE_ERROR\t'+question+'\t'+str(exc),file=sys.stderr)
            return 2
        # Dependency isolation: q3b uses trusted q3a; q3c uses trusted q3a and q3b.
        with tempfile.TemporaryDirectory(prefix='pf1_question_') as scratch:
            isolated=pathlib.Path(scratch)
            source=student/(question+'.py')
            if source.is_file(): shutil.copy2(source,isolated/source.name)
            if question in ('q3b','q3c'): shutil.copy2(reference/'q3a.py',isolated/'q3a.py')
            if question=='q3c': shutil.copy2(reference/'q3b.py',isolated/'q3b.py')
            for i,args in enumerate(cases,1):
                try:
                    with contextlib.redirect_stdout(io.StringIO()):
                        expected=('OK',trusted_function(*copy.deepcopy(args)))
                except Exception as exc:
                    expected=('ERROR',str(exc))
                if expected[0]!='OK':
                    issues.append('Reference failed case '+str(i)); continue
                actual=execute(isolated,question,function,args,5)
                if actual==expected: passed+=1
                else: issues.append('case '+str(i)+': '+actual[0])
        # Avoid awarding marks if no reference cases could be evaluated.
        score=round(maximum*passed/len(cases),3) if cases else 0
        total+=score
        status='Graded successfully' if passed==len(cases) and cases else ('; '.join(issues[:4]) or 'No runnable cases')
        print('\t'.join(map(str,(question,score,maximum,passed,len(cases),status))),flush=True)
    print('TOTAL\t'+str(round(total,3))+'\t25',flush=True)
if __name__=='__main__': main()
