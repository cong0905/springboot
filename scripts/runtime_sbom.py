"""Convert Maven's resolved runtime dependency list, including transitives, to CycloneDX."""
import argparse, json, re
from pathlib import Path
from urllib.parse import quote

parser=argparse.ArgumentParser()
parser.add_argument('dependencies')
parser.add_argument('output')
args=parser.parse_args()
components=[]
for line in Path(args.dependencies).read_text(encoding='utf-8').splitlines():
    match=re.match(r'\s+([^\s:]+):([^\s:]+):jar:(?:[^\s:]+:)?([^\s:]+):(compile|runtime)\b',line)
    if not match: continue
    group, artifact, version, scope=match.groups()
    components.append({'type':'library','group':group,'name':artifact,'version':version,'purl':f'pkg:maven/{quote(group)}/{quote(artifact)}@{quote(version)}'})
if not components: raise SystemExit('No runtime dependencies found. Refusing an empty security scan.')
destination=Path(args.output);destination.parent.mkdir(parents=True,exist_ok=True)
destination.write_text(json.dumps({'bomFormat':'CycloneDX','specVersion':'1.5','version':1,'components':components},indent=2)+'\n',encoding='utf-8')
print(f'Wrote {len(components)} resolved runtime components to {destination}')
