"""Fingerprint repair inputs independently of generated QA and Graphify output."""
import argparse, hashlib, json
from datetime import datetime, timezone
from pathlib import Path

parser=argparse.ArgumentParser()
parser.add_argument('output')
parser.add_argument('--compare',help='Refuse a changed source before publishing final evidence.')
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
inputs=[root/'pom.xml',root/'.gitleaks.toml']
for directory in ('src','scripts','.github'):
    inputs.extend(p for p in (root/directory).rglob('*') if p.is_file() and '__pycache__' not in p.parts)
files={p.relative_to(root).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(inputs)}
if args.compare and json.loads((root/args.compare).read_text(encoding='utf-8'))['files']!=files:
    raise SystemExit('Source changed after verification started; rerun affected validation.')
digest=hashlib.sha256(json.dumps(files,sort_keys=True).encode()).hexdigest()
destination=root/args.output;destination.parent.mkdir(parents=True,exist_ok=True)
destination.write_text(json.dumps({'capturedAt':datetime.now(timezone.utc).isoformat(),'inputCount':len(files),'sourceSha256':digest,'files':files},indent=2)+'\n',encoding='utf-8')
print(f'{len(files)} inputs; source SHA256 {digest}; comparison PASS' if args.compare else f'{len(files)} inputs; source SHA256 {digest}')
