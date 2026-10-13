"""Refresh AST and merge reviewed semantic fragments; run after graphify update .

Semantic docs/UI/image fragments are produced by the Graphify skill, not inferred
from test counts. Multiple evidence records are retained for a single graph edge.
"""
from pathlib import Path
from collections import Counter
import argparse, json, subprocess
from graphify.detect import detect, save_manifest
from graphify.extract import extract
from graphify.build import build_from_json
from graphify.cluster import cluster, score_all
from graphify.analyze import god_nodes, surprising_connections, suggest_questions
from graphify.report import generate
from graphify.export import to_json
from graphify.diagnostics import diagnose_extraction, format_diagnostic_report
from graphify.cli import _stamped_manifest_files
from graphify.cache import save_semantic_cache

root=Path(__file__).resolve().parents[1]
out=root/'graphify-out'
parser=argparse.ArgumentParser()
parser.add_argument('--force-after-audit',action='store_true',help='Only after reviewing every removed node from a full scan.')
arguments=parser.parse_args()
def save(name,data): (out/name).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def read(name): return json.loads((out/name).read_text(encoding='utf-8-sig'))
detection=detect(root)
save('.graphify_detect.json',detection)
semantic={'nodes':[],'edges':[],'hyperedges':[]}
for name in ('.graphify_semantic_docs.json','.graphify_semantic_ui.json','.graphify_semantic_image.json'):
    fragment=read(name)
    for key in semantic: semantic[key].extend(fragment.get(key,[]))
save('.graphify_semantic.json',semantic)
represented_docs={Path(n['source_file']).resolve() for n in semantic['nodes'] if n.get('source_file')}
unrepresented_docs=[Path(p) for p in detection['files'].get('document',[]) if Path(p).resolve() not in represented_docs]
ast=extract([Path(p) for p in detection['files']['code']]+unrepresented_docs,root=root,cache_root=out/'cache',parallel=False)
for n in ast['nodes']:
    if str(n.get('source_file','')).endswith('reference-laravel-features.md'):
        n['provenance_scope']='Legacy Laravel reference; not an implemented TechShop feature'
    if str(n.get('source_file','')).endswith('PROJECT_AUDIT_REPORT.md'):
        n['provenance_scope']='Audit snapshot; may predate subsequent code fixes and test runs'
save('.graphify_ast.json',ast)
node_map={n['id']:n for n in ast['nodes']}
for n in semantic['nodes']: node_map[n['id']]={**n,'assistant_semantic':True}
raw_edges=ast['edges']+semantic['edges']
for edge in raw_edges:
    for key in ('source','target'):
        identity=edge.get(key)
        if identity and identity not in node_map:
            node_map[identity]={'id':identity,'label':str(identity).split('_')[-1],
                'file_type':'external_reference','source_file':edge.get('source_file'),
                'resolution':'Unresolved referenced symbol from AST; declaration not located'}
grouped={}
for edge in raw_edges:
    key=(edge.get('source'),edge.get('target'))
    if not all(key): raise ValueError('Edge without endpoints')
    if key not in grouped: grouped[key]={**edge,'evidence':[]}
    grouped[key]['evidence'].append({k:v for k,v in edge.items() if k not in ('source','target')})
    if edge.get('confidence')=='EXTRACTED': grouped[key]['confidence']='EXTRACTED'
extraction={'nodes':list(node_map.values()),'edges':list(grouped.values()),'hyperedges':semantic['hyperedges']}
save('.graphify_extract.json',extraction)
health=diagnose_extraction(extraction,directed=True,root=str(root))
save('graph-health.json',health)
graph=build_from_json(extraction,root=root,directed=True)
if not graph.number_of_nodes(): raise ValueError('Empty graph')
communities=cluster(graph)
names={'ai':'Gemini AI Advice and Quotas','chat':'Customer and Admin Chat','identity':'Identity and Sessions','catalog':'Catalog and Images','cart':'Cart and Pricing',
 'variant':'Variants and Specifications','branch':'Branches and Inventory','imei':'IMEI Lifecycle',
 'order':'Checkout and Orders','inventory':'Stock Movements','coupon':'Coupons and Discounts',
 'review':'Product Reviews','payment':'Payment and VNPay','reporting':'Commerce Reporting',
 'tradein':'Trade In Valuation','installment':'Installment Applications','loyalty':'Member Loyalty',
 'warranty':'Warranty and Repairs','content':'News and Newsletter','guest':'Guest Cart and Order Proofs','config':'Security and Web Controllers','shared':'Validation and Errors'}
def domain(n):
    source=str(graph.nodes[n].get('source_file','')).replace('\\','/').lower()
    if '/vn/techshop/' in source:
        package=source.split('/vn/techshop/')[1].split('/')[0]
        return names.get(package,'Commerce Integration Tests')
    if '/templates/' in source:return 'Commerce Web Pages'
    if '/db/' in source or source.endswith('.sql'):return 'Database Schema and Migrations'
    if '/scripts/' in source:return 'Build and Verification Scripts'
    if source.endswith('.svg'):return 'Product Illustrations'
    return 'Design and Delivery Guides'
labels={cid:Counter(domain(n) for n in members).most_common(1)[0][0] for cid,members in communities.items()}
cohesion=score_all(graph,communities)
gods=god_nodes(graph);surprises=surprising_connections(graph,communities);questions=suggest_questions(graph,communities,labels)
if arguments.force_after_audit and (out/'graph.json').exists():
    previous=read('graph.json')
    removed=[n for n in previous['nodes'] if n['id'] not in graph]
    save('rebuild-removals.json',{'reason':'Full AST+reviewed semantic rebuild; prior incremental Markdown heading nodes replaced by reviewed document concepts, unresolved import stubs are not current nodes. Inspect this list before using this flag.','removed':removed})
if not to_json(graph,communities,str(out/'graph.json'),community_labels=labels,force=arguments.force_after_audit):
    raise SystemExit('Graph shrink guard rejected output; inspect before forcing.')
report=generate(graph,communities,cohesion,labels,gods,surprises,detection,{},str(root),suggested_questions=questions)
report=report.replace('- Token cost: 0 input · 0 output','- Token usage: unavailable; agent tools did not expose usage counters.')
report+='\n## Integrity and extraction limits\n\n'
report+=f'- {len(raw_edges)} raw edges are represented by {len(grouped)} endpoint pairs; every evidence record is retained on its edge.\n'
report+=f'- Self-loops: {health.get("self_loop_edges",0)}. Java overloads can share AST IDs; Python schema() is recursive. Treat self-loops as an extractor caveat rather than proof of recursion.\n'
report+='- SVG semantics were inspected from XML sources; view_image does not support SVG. Runtime SVG render was checked with browser screenshots.\n'
report+='- Roadmap and baseline docs are targets; implementation and QA evidence remain authoritative.\n'
report+='- '+format_diagnostic_report(health).replace('\n','\n  ')+'\n'
(out/'GRAPH_REPORT.md').write_text(report,encoding='utf-8')
save('.graphify_labels.json',{str(k):v for k,v in labels.items()})
save('.graphify_analysis.json',{'communities':{str(k):v for k,v in communities.items()},'cohesion':{str(k):v for k,v in cohesion.items()},'gods':gods,'surprises':surprises,'questions':questions})
corpus=detection['files']; stamped=_stamped_manifest_files(corpus,extraction,root)
all_files={f for values in corpus.values() for f in values}
dispatched={f for typ,values in corpus.items() if typ in ('document','paper','image') for f in values}
represented={f for values in stamped.values() for f in values}
save_manifest(stamped,manifest_path=str(out/'manifest.json'),root=root,scan_corpus=all_files,clear_semantic=dispatched-represented or None)
spec=Path.home()/'.codex/skills/graphify/references/extraction-spec.md'
save_semantic_cache(semantic['nodes'],semantic['edges'],semantic['hyperedges'],root=root,allowed_source_files=represented,prompt_file=spec,cache_root=out/'cache')
for kind in ('html','wiki'):
    subprocess.run(['graphify','export',kind,'--graph',str(out/'graph.json'),'--labels',str(out/'.graphify_labels.json')],cwd=root,check=True)
print(json.dumps({'nodes':graph.number_of_nodes(),'edges':graph.number_of_edges(),'communities':len(communities),'health':{k:health[k] for k in ('dangling_endpoint_edges','missing_endpoint_edges','self_loop_edges','directed_same_endpoint_collapsed_edges')}},ensure_ascii=False))
