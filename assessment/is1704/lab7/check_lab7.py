"""Lab 7 Q2/Q4 completion checker. Run ONLY inside a restricted sandbox.
Usage: python check_lab7.py --submissions DIR --output CSV
Accepts .zip, .py, .ipynb and directories. Unknown identity -> review.
"""
import argparse, ast, csv, importlib.util, io, json, os, re, shutil, subprocess, sys, tempfile, zipfile
from pathlib import Path
from collections import Counter

ROOT=Path(__file__).resolve().parent
RES=ROOT/'resources'
PARTS={'Q2a':('q2a.py','get_universities_founded_before'),'Q2b':('q2b.py','get_average_num_students'),'Q2c':('q2c.py','search_with_keyword'),'Q4a':('q4a.py',None),'Q4b':('q4b.py',None)}
LIMIT=2_000_000

def notebook_parts(path, out):
    try: nb=json.loads(path.read_text(encoding='utf-8-sig'))
    except Exception: return ['Unreadable notebook: '+path.name]
    problems=[]
    cells=[(''.join(c.get('source',[]))) for c in nb.get('cells',[]) if c.get('cell_type')=='code']
    for label,(name,func) in PARTS.items():
        if (out/name).exists(): continue
        matches=[]
        for code in cells:
            if func:
                try: tree=ast.parse(code)
                except SyntaxError: continue
                if any(isinstance(n,(ast.FunctionDef,ast.AsyncFunctionDef)) and n.name==func for n in tree.body): matches.append(code)
            elif re.search(r'(?m)^\s*#\s*Q4\s*'+label[-1]+r'\b',code,re.I): matches.append(code)
        if len(matches)==1: (out/name).write_text(matches[0],encoding='utf-8')
        elif len(matches)>1: problems.append('Ambiguous notebook cells for '+label)
    return problems

def safe_zip(z, dest):
    problems=[]; seen=set(); count=0; size=0
    with zipfile.ZipFile(z) as archive:
        for entry in archive.infolist():
            if entry.is_dir(): continue
            count+=1; size+=entry.file_size
            if count>200 or size>15_000_000: raise ValueError('ZIP exceeds safety limits')
            p=Path(entry.filename.replace('\\','/'))
            if p.is_absolute() or '..' in p.parts or re.match(r'^[a-zA-Z]:',entry.filename): raise ValueError('Unsafe ZIP entry')
            if p.suffix.lower() not in ('.py','.ipynb'): continue
            if entry.file_size>LIMIT: raise ValueError('File exceeds size limit')
            name=p.name.lower()
            if name in seen: problems.append('Duplicate file: '+name);continue
            seen.add(name)
            with archive.open(entry) as source, (dest/p.name).open('wb') as target: shutil.copyfileobj(source,target)
    return problems

def normalize(source,dest):
    problems=[]
    if source.is_dir():
        for p in source.rglob('*'):
            if p.is_file() and p.suffix.lower() in ('.py','.ipynb'):
                if p.stat().st_size>LIMIT: problems.append('Oversized file: '+p.name);continue
                target=dest/p.name
                if target.exists(): problems.append('Duplicate file: '+p.name);continue
                shutil.copy2(p,target)
    elif source.suffix.lower()=='.zip': problems+=safe_zip(source,dest)
    elif source.suffix.lower() in ('.py','.ipynb'): shutil.copy2(source,dest/source.name)
    for nb in list(dest.glob('*.ipynb')): problems+=notebook_parts(nb,dest)
    return problems

def run_child(work,part):
    # Separate process per part, so infinite loops/errors cannot block the batch.
    code=r'''
import importlib.util,json,sys,math
from pathlib import Path
part=sys.argv[1]
name,func={'Q2a':('q2a.py','get_universities_founded_before'),'Q2b':('q2b.py','get_average_num_students'),'Q2c':('q2c.py','search_with_keyword')}[part]
spec=importlib.util.spec_from_file_location('student_answer',name)
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
f=getattr(m,func)
if part=='Q2a':
 cases=[('universities-1.txt',1905),('universities-2.txt',1800),('universities-1.txt',1000)]
elif part=='Q2b':cases=[('universities-1.txt',),('universities-2.txt',),('empty-universities.txt',)]
else:cases=[('universities-1.txt','california'),('universities-1.txt','CALIFORNIA'),('universities-2.txt','university'),('universities-1.txt','no-such-university')]
for args in cases:
 try:
  val=f(*args)
  print(json.dumps({'ok':True,'value':val}))
 except Exception as e: print(json.dumps({'ok':False,'error':type(e).__name__+': '+str(e)}))
'''
    return subprocess.run([sys.executable,'-I','-c',code,part],cwd=work,capture_output=True,text=True,timeout=5)

def expected(part,work):
    # Trusted reference executes in separate directory, using same input files.
    return run_child(work,part)

def compare_lines(part,actual,ref):
    if part=='Q4a':
        return actual.replace('\r\n','\n').strip()==ref.replace('\r\n','\n').strip()
    # Q4b month order can differ: compare month blocks irrespective of ordering.
    def blocks(text):
        found={}; key=None
        for line in text.replace('\r\n','\n').splitlines():
            if re.match(r'^\d{2}-\d{4}: total transaction amount is \$',line):
                key=line.split(':',1)[0];found[key]=[line.split(':',1)[1].strip()]
            elif key is not None and line.strip():found[key].append(line.strip())
        return found
    a,b=blocks(actual),blocks(ref)
    if a.keys()!=b.keys():return False
    for month in a:
        try:
            at=float(a[month][0].split('$')[-1]);bt=float(b[month][0].split('$')[-1])
        except ValueError:return False
        if abs(at-bt)>0.011 or Counter(a[month][1:])!=Counter(b[month][1:]):return False
    return bool(a)

def evaluate(part,student,reference):
    file,func=PARTS[part]
    if not (student/file).is_file():return 'Missing','Required answer '+file+' not found'
    if func:
        try: a=run_child(student,part); b=expected(part,reference)
        except subprocess.TimeoutExpired:return 'Needs Review','Execution timeout; check loops or blocking input'
        if a.returncode or b.returncode:return 'Needs Review','Could not import/run function: '+(a.stderr or b.stderr)[-180:].replace('\n',' ')
        try: ar=[json.loads(x) for x in a.stdout.splitlines()];br=[json.loads(x) for x in b.stdout.splitlines()]
        except Exception:return 'Needs Review','Unexpected printed output during function tests'
        if len(ar)!=len(br):return 'Needs Review','Unexpected number of test responses'
        failures=[]
        for i,(x,y) in enumerate(zip(ar,br),1):
            if not x['ok'] or x.get('value')!=y.get('value'):
                if part=='Q2b' and x['ok'] and y['ok'] and isinstance(x.get('value'),(float,int)) and abs(x['value']-y['value'])<1e-6:continue
                failures.append(str(i))
        return ('Correct','All selected automated cases passed') if not failures else ('Incorrect','Failed test case(s): '+', '.join(failures)+'; review function logic and edge cases')
    outfile='transactions-output-1.txt' if part=='Q4a' else 'transactions-output-2.txt'
    try:
        a=subprocess.run([sys.executable,'-I',file],cwd=student,capture_output=True,text=True,timeout=5)
        b=subprocess.run([sys.executable,'-I',file],cwd=reference,capture_output=True,text=True,timeout=5)
    except subprocess.TimeoutExpired:return 'Needs Review','Script timed out'
    if a.returncode:return 'Needs Review','Script error: '+a.stderr[-180:].replace('\n',' ')
    if b.returncode:return 'Needs Review','Reference script error; check setup'
    if not (student/outfile).exists():return 'Incorrect','Expected output file '+outfile+' was not generated'
    ok=compare_lines(part,(student/outfile).read_text(errors='replace'),(reference/outfile).read_text(errors='replace'))
    return ('Correct','Output matches reference for supplied transaction data') if ok else ('Incorrect','Generated transaction file differs from expected results')

def check(source):
    with tempfile.TemporaryDirectory(prefix='lab7_') as td:
        base=Path(td);student=base/'student';ref=base/'reference';student.mkdir();ref.mkdir()
        problems=normalize(source,student)
        for p in RES.iterdir():
            if p.is_file():
                for d in (student,ref):
                    if not (d/p.name).exists():shutil.copy2(p,d/p.name)
        (student/'empty-universities.txt').write_text('')
        (ref/'empty-universities.txt').write_text('')
        for f in (ROOT/'reference').glob('*.py'):shutil.copy2(f,ref/f.name)
        result={'Submission':source.name,'Student ID':'Unverified' if source.stem.lower().startswith('lab') else source.stem,'Normalization Notes':'; '.join(problems)}
        for part in PARTS:
            status,feedback=evaluate(part,student,ref)
            if problems and status=='Missing':status='Needs Review';feedback+='; '+ '; '.join(problems)
            result[part+' Status']=status;result[part+' Feedback']=feedback
        return result

def main():
    p=argparse.ArgumentParser();p.add_argument('--submissions',required=True);p.add_argument('--output',required=True);args=p.parse_args()
    folder=Path(args.submissions)
    if not folder.is_dir():p.error('Submission folder not found')
    items=[x for x in sorted(folder.iterdir()) if x.is_dir() or x.suffix.lower() in ('.zip','.py','.ipynb')]
    rows=[]
    for item in items:
        try:row=check(item)
        except Exception as ex:row={'Submission':item.name,'Student ID':'Unverified','Normalization Notes':'Needs Review: '+str(ex)}
        rows.append(row)
        print(item.name, ' | '.join(p+': '+row.get(p+' Status','Needs Review') for p in PARTS),flush=True)
    cols=['Submission','Student ID','Normalization Notes']+[k for part in PARTS for k in (part+' Status',part+' Feedback')]
    target=Path(args.output);target.parent.mkdir(parents=True,exist_ok=True)
    with target.open('w',encoding='utf-8-sig',newline='') as f:
        w=csv.DictWriter(f,fieldnames=cols);w.writeheader();w.writerows(rows)
    print('Saved:',target.resolve())
if __name__=='__main__':main()
