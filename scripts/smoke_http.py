"""Manual smoke test: run a fresh demo profile at 127.0.0.1:8080 first.
Uses only standard-library Python 3; changes the disposable demo database.
"""
import urllib.request,urllib.parse,urllib.error,http.cookiejar,json
from html.parser import HTMLParser
base='http://127.0.0.1:8080'
class Inputs(HTMLParser):
 def __init__(self,text):super().__init__();self.inputs=[];self.feed(text)
 def handle_starttag(self,tag,attrs):
  if tag=='input':self.inputs.append(dict(attrs))
class Session:
 def __init__(self):self.client=urllib.request.build_opener(urllib.request.ProxyHandler({}),urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
 def request(self,path,data=None,headers=None):
  req=urllib.request.Request(base+path,data=data,headers=headers or {})
  r=self.client.open(req,timeout=10);return r,r.read().decode()
 def getjson(self,path):r,text=self.request(path);return json.loads(text)
 def mutate(self,path,data):
  token=self.getjson('/api/v1/csrf');r,text=self.request(path,json.dumps(data,ensure_ascii=False).encode(),{token['headerName']:token['token'],'Content-Type':'application/json'});return r,json.loads(text)
def login(email,password):
 s=Session();r,text=s.request('/login');csrf=next(i['value'] for i in Inputs(text).inputs if i.get('name')=='_csrf');r,text=s.request('/login',urllib.parse.urlencode({'username':email,'password':password,'_csrf':csrf}).encode(),{'Content-Type':'application/x-www-form-urlencoded'});assert r.geturl().endswith('/admin' if email.startswith('admin') else '/products'),r.geturl();return s
s=login('customer@techshop.test','DemoCustomer2026!');cart=s.getjson('/api/v1/cart');r,cart=s.mutate('/api/v1/cart/items',{'productId':1,'quantity':1,'expectedVersion':cart['version']});assert r.status==200
r,text=s.request('/checkout');names={'checkoutKey','cartVersion','expectedTotal','expectedPricingHash'};data={i['name']:i['value'] for i in Inputs(text).inputs if i.get('name') in names};data['cartVersion']=int(data['cartVersion']);data['expectedTotal']=int(data['expectedTotal']);data.update(recipientName='Khách kiểm thử',phone='0900000000',address='Địa chỉ giả lập để kiểm thử HTTP',note=None)
r,order=s.mutate('/api/v1/orders',data);assert r.status==201;r,replay=s.mutate('/api/v1/orders',data);assert r.status==200 and r.headers['Idempotent-Replay']=='true' and replay['id']==order['id']
a=login('admin@techshop.test','DemoAdmin2026!')
for state in ['CONFIRMED','SHIPPED','DELIVERED']:
 r,order=a.mutate(f"/api/v1/admin/orders/{order['id']}/transitions",{'targetStatus':state,'expectedVersion':order['version'],'codCollected':state=='DELIVERED'});assert r.status==200
report=a.getjson('/api/v1/admin/reports/summary');assert report['deliveredOrders']==1 and report['merchandiseRevenue']==1290000,report
for session,paths in [(s,['/products','/cart','/orders',f"/orders/{order['id']}"]),(a,['/admin','/admin/categories','/admin/products','/admin/products/1/edit','/admin/orders',f"/admin/orders/{order['id']}"])]:
 for path in paths:r,text=session.request(path);assert r.status==200,path
print('PASS: real HTTP login, session/CSRF, cart, HTML checkout preview, COD checkout/replay, admin delivery/report, customer/admin pages')
