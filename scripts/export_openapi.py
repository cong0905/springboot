"""Export the implemented omnichannel contracts; run after changing its records/routes."""
from pathlib import Path
import json,re
ROOT=Path(__file__).resolve().parents[1]
sources={p.stem:p.read_text(encoding='utf-8') for p in (ROOT/'src/main/java/vn/techshop').rglob('*.java')}
records={}
def split_fields(text):
    fields=[];start=0;depth=0;quote=False;escape=False
    for i,c in enumerate(text):
        if quote:
            if escape:escape=False
            elif c=='\\':escape=True
            elif c=='"':quote=False
        elif c=='"':quote=True
        elif c in '(<':depth+=1
        elif c in ')>':depth-=1
        elif c==',' and depth==0:fields.append(text[start:i]);start=i+1
    fields.append(text[start:]);return fields
for owner,source in sources.items():
    for match in re.finditer(r'public record (\w+)\(',source):
        start=match.end();depth=1;end=start;quote=False;escape=False
        while end<len(source) and depth:
            c=source[end]
            if quote:
                if escape:escape=False
                elif c=='\\':escape=True
                elif c=='"':quote=False
            elif c=='"':quote=True
            elif c=='(':depth+=1
            elif c==')':depth-=1
            end+=1
        records[owner+'_'+match.group(1)]=(owner,split_fields(source[start:end-1]))
def schema(typ,owner):
    typ=typ.strip()
    if typ.startswith('List<'):return {'type':'array','items':schema(typ[5:-1],owner)}
    if typ in ['Long','long','Integer','int']:return {'type':'integer','format':'int64'}
    if typ in ['boolean','Boolean']:return {'type':'boolean'}
    if typ in ['Double','double','BigDecimal']:return {'type':'number'}
    if typ in ['Instant','LocalDate']:return {'type':'string','format':'date-time' if typ=='Instant' else 'date'}
    if typ=='String':return {'type':'string'}
    key=typ.replace('.','_') if '.' in typ else owner+'_'+typ
    if key in records:return {'$ref':'#/components/schemas/'+key}
    return {'type':'object'}
schemas={}
for key,(owner,fields) in records.items():
    props={};required=[]
    for f in fields:
        cleaned=re.sub(r'@\w+(?:\([^)]*\))?','',f).strip()
        parts=cleaned.rsplit(None,1)
        if len(parts)!=2:continue
        typ,name=parts;props[name]=schema(typ,owner)
        if '@NotBlank' in f or '@NotNull' in f or '@NotEmpty' in f:required.append(name)
    schemas[key]={'type':'object','properties':props,'additionalProperties':False}
    if required:schemas[key]['required']=required
paths={}
for m in re.finditer(r'@(Get|Post|Patch)Mapping\("([^"\n]+)"\)\s+(.*?)\((.*?)\)\s*\{',sources['OmnichannelApi'],re.S):
    method,route,head,args=m.groups();method=method.lower();route='/api/v1'+route
    result_type,operation=head.strip().rsplit(None,1)
    op={'operationId':operation,'summary':operation,'responses':{'200':{'description':'Success','content':{'application/json':{'schema':schema(result_type,'OmnichannelApi')}}},'400':{'description':'Validation error'},'401':{'description':'Session required'},'403':{'description':'Role or CSRF rejected'},'404':{'description':'Resource unavailable'},'409':{'description':'Concurrent/state conflict'}}}
    if 'ResponseEntity<Void>' in result_type:op['responses'].pop('200');op['responses']['204']={'description':'Updated without body'}
    params=[]
    for name in re.findall(r'\{(\w+)\}',route):params.append({'name':name,'in':'path','required':True,'schema':{'type':'integer','format':'int64'}})
    for q in re.finditer(r'@RequestParam(\([^)]*\))?\s+(\S+)\s+(\w+)',args):
        opts,typ,name=q.groups();params.append({'name':name,'in':'query','required':'required=false' not in (opts or '') and 'defaultValue=' not in (opts or ''),'schema':schema(typ,'OmnichannelApi')})
    if params:op['parameters']=params
    b=re.search(r'@RequestBody\s+(\S+)\s+\w+',args)
    if b:op['requestBody']={'required':True,'content':{'application/json':{'schema':schema(b.group(1),'OmnichannelApi')}}}
    op['security']=[] if method=='get' and '/admin/' not in route and '/me/' not in route else [{'session':[]}]
    if method in ['post','patch']:op['parameters']=op.get('parameters',[])+[{'name':'X-CSRF-TOKEN','in':'header','required':True,'schema':{'type':'string'}}]
    if '/admin/' in route:op['description']='Requires ROLE_ADMIN.'
    paths.setdefault(route,{})[method]=op
paths['/api/v1/orders']={'post':{'operationId':'checkout','summary':'Transactional checkout; replay returns 200, creation 201','security':[{'session':[]}],'parameters':[{'name':'X-CSRF-TOKEN','in':'header','required':True,'schema':{'type':'string'}}],'requestBody':{'required':True,'content':{'application/json':{'schema':{'$ref':'#/components/schemas/OrderService_Checkout'}}}},'responses':{str(s):{'description':d} for s,d in [(201,'Order created'),(200,'Idempotent replay'),(400,'Invalid input'),(403,'Role/CSRF'),(409,'Price/cart/stock/state changed')]}}}
for prefix,role in [('/api/v1/orders','CUSTOMER (owner)'),('/api/v1/admin/orders','ADMIN')]:
    paths[prefix+'/{id}/pickup-qr.png']={'get':{'operationId':'pickupQr' if role.startswith('CUSTOMER') else 'adminPickupQr','summary':'PNG QR containing pickup code','description':f'Requires {role}. Active pickup orders only. Expired unpaid holds return 409. Cache-Control: no-store.','security':[{'session':[]}],'parameters':[{'name':'id','in':'path','required':True,'schema':{'type':'integer','format':'int64'}}],'responses':{'200':{'description':'256x256 PNG QR','content':{'image/png':{'schema':{'type':'string','format':'binary'}}}},'401':{'description':'Authentication required'},'403':{'description':'Role denied'},'404':{'description':'Unknown, other owner or ineligible order'},'409':{'description':'PICKUP_EXPIRED'}}}}
doc={'openapi':'3.0.3','info':{'title':'TechShop Omnichannel implemented API','version':'0.2.1','description':'Session authentication. Fetch /api/v1/csrf using the same cookie; send X-CSRF-TOKEN for mutations. Checkout snapshot uses POST /api/v1/orders/preview. Money is integer VND. Installments are internal demo approvals. Public warranty lookup uses serial; phone is an optional matching proof. Branch filters use configured provinceCode/districtCode values; inStock=true hides empty stores. Order detail includes resumable paymentUrl for its persisted transaction.'},'servers':[{'url':'http://localhost:8080'}],'paths':paths,'components':{'securitySchemes':{'session':{'type':'apiKey','in':'cookie','name':'JSESSIONID'}},'schemas':schemas}}
(ROOT/'docs/openapi.json').write_text(json.dumps(doc,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'OpenAPI: {len(paths)} paths, {sum(len(v) for v in paths.values())} operations, {len(schemas)} schemas')
