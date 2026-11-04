"""Prepare an LMS bulk ZIP for Lab 7 Q2/Q4 checks. Never executes student code."""
import argparse, csv, io, re, shutil, tarfile, zipfile
from pathlib import Path

QUESTIONS = ('q2a.py','q2b.py','q2c.py','q4a.py','q4b.py')
MAX_ENTRY = 3_000_000
MAX_ARCHIVE = 25_000_000
MAX_FILES = 300


def roster_rows(path):
    with open(path, newline='', encoding='utf-8-sig') as f:
        rows=list(csv.DictReader(f))
    if not rows or not {'username','lms_id'} <= set(rows[0]):
        raise ValueError('Roster must have username and lms_id columns')
    names=[r['username'].strip() for r in rows]
    ids=[r['lms_id'].strip() for r in rows if r['lms_id'].strip()]
    if len(names)!=len(set(names)) or len(ids)!=len(set(ids)):
        raise ValueError('Duplicate username or LMS ID in roster')
    return rows


def unpack_zip_bytes(blob):
    with zipfile.ZipFile(io.BytesIO(blob)) as z:
        entries=z.infolist()
        if len(entries)>MAX_FILES or sum(e.file_size for e in entries)>MAX_ARCHIVE:
            raise ValueError('Archive too large')
        for e in entries:
            if e.is_dir(): continue
            if e.file_size>MAX_ENTRY: continue
            path=Path(e.filename.replace('\\','/'))
            if path.is_absolute() or '..' in path.parts or re.match(r'^[a-zA-Z]:',e.filename): continue
            if path.suffix.lower() in ('.py','.ipynb'):
                yield path.name, z.read(e)


def unpack_tar_bytes(blob):
    with tarfile.open(fileobj=io.BytesIO(blob),mode='r:gz') as t:
        entries=t.getmembers()
        if len(entries)>MAX_FILES or sum(e.size for e in entries)>MAX_ARCHIVE:
            raise ValueError('Archive too large')
        for e in entries:
            if not e.isfile() or e.size>MAX_ENTRY:continue
            path=Path(e.name.replace('\\','/'))
            if path.is_absolute() or '..' in path.parts:continue
            if path.suffix.lower() in ('.py','.ipynb'):
                stream=t.extractfile(e)
                if stream:yield path.name,stream.read()


def matching_question(filename):
    low=filename.lower()
    matches=[q for q in QUESTIONS if re.search(r'(?<![a-z0-9])'+re.escape(q[:-3])+r'\.py$',low)]
    return matches[0] if len(matches)==1 else None


def main():
    p=argparse.ArgumentParser()
    p.add_argument('--archive',required=True)
    p.add_argument('--roster',required=True)
    p.add_argument('--output',required=True)
    args=p.parse_args()
    roster=roster_rows(args.roster)
    target=Path(args.output).resolve()
    if target.exists() and any(target.iterdir()):
        p.error('Output folder must be new or empty; originals are never overwritten')
    target.mkdir(parents=True,exist_ok=True)
    users={r['username'].strip():r for r in roster}
    byid={r['lms_id'].strip():r['username'].strip() for r in roster if r['lms_id'].strip()}
    received={u:[] for u in users}
    notes={u:[] for u in users}
    unknown=[]
    with zipfile.ZipFile(args.archive) as bulk:
        for entry in bulk.infolist():
            if entry.is_dir() or entry.filename=='index.html':continue
            name=Path(entry.filename).name
            if entry.file_size>MAX_ARCHIVE:
                unknown.append((name,'Oversized LMS entry'));continue
            prefix=re.match(r'^(\d+)-\d+\s+-',name)
            username=byid.get(prefix.group(1)) if prefix else None
            if not username:
                # Exact username matching only; never guess based on student name.
                candidates=[u for u in users if u.lower() in name.lower()]
                if len(candidates)==1:username=candidates[0]
            if not username:
                unknown.append((name,'Could not match LMS ID to roster'));continue
            received[username].append(name)
            folder=target/username
            folder.mkdir(exist_ok=True)
            try:
                blob=bulk.read(entry)
                if name.lower().endswith('.tar.gz'):files=list(unpack_tar_bytes(blob))
                elif name.lower().endswith('.zip'):files=list(unpack_zip_bytes(blob))
                elif name.lower().endswith(('.py','.ipynb')):files=[(name,blob)]
                else:
                    notes[username].append('Unsupported file: '+name);continue
                for filename,data in files:
                    question=matching_question(filename)
                    if question:
                        dest=folder/question
                        if dest.exists():
                            notes[username].append('Duplicate '+question+'; kept first copy');continue
                        dest.write_bytes(data)
                    elif filename.lower().endswith('.ipynb'):
                        dest=folder/filename
                        if dest.exists():notes[username].append('Duplicate notebook: '+filename)
                        else:dest.write_bytes(data)
            except Exception as exc:
                notes[username].append('Could not unpack '+name+': '+str(exc))
    manifest=target/'submission_manifest.csv'
    with manifest.open('w',newline='',encoding='utf-8-sig') as f:
        fields=['Username','Submission Status',*QUESTIONS,'Notes','Original LMS Filenames']
        writer=csv.DictWriter(f,fieldnames=fields);writer.writeheader()
        for username in users:
            folder=target/username
            folder.mkdir(exist_ok=True)
            row={'Username':username,'Submission Status':'Submitted' if received[username] else 'No Submission',
                 'Notes':'; '.join(notes[username]),'Original LMS Filenames':'; '.join(received[username])}
            row.update({q:('No Submission' if not received[username] else ('Found' if (folder/q).exists() else 'Missing')) for q in QUESTIONS})
            writer.writerow(row)
    with (target/'unmatched_files.csv').open('w',newline='',encoding='utf-8-sig') as f:
        w=csv.writer(f);w.writerow(['LMS Filename','Reason']);w.writerows(unknown)
    print('Roster:',len(users),'| Submitted:',sum(bool(v) for v in received.values()),'| Missing:',sum(not v for v in received.values()))
    print('Unmatched files:',len(unknown))
    print('Normalized:',target)
    print('Manifest:',manifest)

if __name__=='__main__':main()