"""Capture actual Surefire XML before another database run overwrites it."""
from pathlib import Path
from datetime import datetime, timezone
import argparse, json, re, xml.etree.ElementTree as ET

parser=argparse.ArgumentParser()
parser.add_argument('--database',required=True)
parser.add_argument('--output',required=True)
parser.add_argument('--log',required=True)
parser.add_argument('--packaging-log')
parser.add_argument('--reports',default='target/surefire-reports',help='Surefire directory for an isolated build')
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
reports=sorted((root/args.reports).glob('TEST-*.xml'))
if not reports: raise SystemExit('No Surefire XML; run tests first.')
log=(root/args.log).read_text(encoding='utf-8',errors='replace')
executed=set(re.findall(r'\[INFO\] Running (\S+)',log))
if not executed: raise SystemExit('No executed test suites in this log; refusing stale XML evidence.')
suites=[];ignored=[]
for path in reports:
    xml=ET.parse(path).getroot()
    if xml.attrib['name'] not in executed:
        ignored.append(xml.attrib['name']);continue
    suites.append({'suite':xml.attrib['name'],**{k:int(xml.attrib.get(k,0)) for k in ('tests','failures','errors','skipped')},'cases':[c.attrib['name'] for c in xml.findall('testcase')]})
totals={k:sum(s[k] for s in suites) for k in ('tests','failures','errors','skipped')}
if {s['suite'] for s in suites}!=executed: raise SystemExit('Missing XML for an executed suite.')
summaries=re.findall(r'Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)',log)
if not summaries or tuple(totals[k] for k in ('tests','failures','errors','skipped'))!=tuple(map(int,summaries[-1])):
    raise SystemExit('Surefire XML totals do not match the final Maven summary.')
packaging=(root/args.packaging_log).read_text(encoding='utf-8',errors='replace') if args.packaging_log else log
data={'database':args.database,'capturedAt':datetime.now(timezone.utc).isoformat(),'testCommandLog':args.log,'packagingLog':args.packaging_log or args.log,'testCommandBuildSuccess':'BUILD SUCCESS' in log,'buildSuccess':'BUILD SUCCESS' in packaging,'totals':totals,'suites':suites,'ignoredStaleSuites':ignored}
(root/args.output).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:v for k,v in data.items() if k!='suites'}))
if totals['failures'] or totals['errors'] or not data['buildSuccess']: raise SystemExit(1)
