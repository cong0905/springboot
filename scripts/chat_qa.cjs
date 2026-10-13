/* Execute against an isolated demo, never against a customer database. */
'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const base=process.env.QA_BASE_URL||'http://localhost:8096';
const out=path.resolve(__dirname,'../qa/chat/browser');fs.mkdirSync(out,{recursive:true});
const evidence={base,checkedAt:new Date().toISOString(),checks:[],pages:[],errors:[]};
const record=name=>{evidence.checks.push({name,result:'PASS'});console.log('PASS '+name);};
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.CHROME_PATH?{executablePath:process.env.CHROME_PATH}:{})});
 let last;
 try{
  const customer=await browser.newContext(),admin=await browser.newContext(),other=await browser.newContext();
  const c=await customer.newPage(),a=await admin.newPage(),o=await other.newPage();last=c;
  for(const p of [c,a,o])p.on('pageerror',e=>evidence.errors.push(e.message));
  async function register(context,name){const csrf=await context.request.get(base+'/api/v1/csrf').then(r=>r.json());const email='chat-qa-'+Date.now()+'-'+Math.random().toString(16).slice(2)+'@test.local';const result=await context.request.post(base+'/api/v1/auth/register',{headers:{[csrf.headerName]:csrf.token},data:{fullName:name,email,password:'ChatBrowser2026!'}});assert.equal(result.status(),201);return email;}
  async function login(page,email,password){await page.goto(base+'/login?redirect=/chat');await page.locator('[name=username]').fill(email);await page.locator('[name=password]').fill(password);await Promise.all([page.waitForURL(url=>!url.pathname.startsWith('/login')),page.locator('form[action="/login"] button').click()]);}
  async function go(page,url){last=page;const response=await page.goto(base+url);assert.equal(response.status(),200,url);}
  async function waitBubble(page,text){await page.locator('.chat-bubble').filter({hasText:text}).first().waitFor({timeout:18000});}
  async function send(page,text){await page.locator('[name=content]').fill(text);await page.locator('[data-chat-form] button[type=submit]').click();await waitBubble(page,text);}
  const email=await register(customer,'Khách QA Chat'),otherEmail=await register(other,'Khách QA Riêng');
  await login(c,email,'ChatBrowser2026!');assert.equal(new URL(c.url()).pathname,'/chat');assert.equal(await c.locator('.admin-nav').count(),0);assert.equal(await c.locator('[data-chat-launcher]').isVisible(),false);
  await login(a,'admin@techshop.test','DemoAdmin2026!');await go(a,'/admin/chat');
  await send(c,'Xin chào, tư vấn giúp tôi chọn điện thoại.');
  const conversation=a.locator('[data-conversation-id]').filter({hasText:'Khách QA Chat'});await conversation.waitFor({timeout:18000});
  await conversation.locator('.chat-unread').waitFor({timeout:18000});await conversation.click();await waitBubble(a,'Xin chào, tư vấn');
  await c.locator('.chat-message-meta').filter({hasText:'Đã xem'}).first().waitFor({timeout:18000});
  await send(a,'Chào bạn! Bạn muốn chọn máy trong khoảng ngân sách nào?');await waitBubble(c,'Chào bạn!');
  record('Two browser sessions: customer/admin send and receive, unread badge and read receipt');
  const unsafe='<img src=x onerror="window.chatUnsafe=true">';await send(c,unsafe);await waitBubble(a,unsafe);
  assert.equal(await a.evaluate(()=>window.chatUnsafe),undefined);assert.equal(await a.locator('.chat-bubble img').count(),0);record('Message HTML displays as literal text without script execution');
  await login(o,otherEmail,'ChatBrowser2026!');assert.equal(await o.locator('.chat-bubble').count(),0);assert.equal((await other.request.get(base+'/api/v1/admin/chat/conversations')).status(),403);record('Another customer cannot see messages or enter admin inbox');
  await go(c,'/products');await c.locator('[data-chat-launcher]').click();await waitBubble(c,'Chào bạn!');record('History survives leaving/reopening chat through storefront launcher');
  // Commit the request on the server, then abort its response to simulate a lost response.
  const retryText='Kiểm tra gửi lại khi mất phản hồi '+Date.now();
  await c.route('**/api/v1/chat/messages',async route=>{if(route.request().method()==='POST'){await route.fetch();await route.abort('failed');await c.unroute('**/api/v1/chat/messages');}else await route.continue();});
  await c.locator('[name=content]').fill(retryText);await c.locator('[data-chat-form] button[type=submit]').click();
  await c.locator('[data-chat-error]').filter({hasText:'giữ lại'}).waitFor({timeout:10000});assert.equal(await c.locator('[name=content]').inputValue(),retryText);
  await c.locator('[data-chat-form] button[type=submit]').click();await waitBubble(c,retryText);
  await c.waitForFunction(()=>document.querySelector('[name=content]').value==='');
  const history=await customer.request.get(base+'/api/v1/chat/messages').then(r=>r.json());assert.equal(history.messages.filter(m=>m.content===retryText).length,1);record('Lost server response retains draft; retry stores exactly one message');
  for(const width of [320,390,768,1440])for(const [page,url,role] of [[c,'/chat','customer'],[a,'/admin/chat','admin']]){
   await page.setViewportSize({width,height:1000});await go(page,url);
   if(role==='admin'){await page.locator('[data-conversation-id]').filter({hasText:'Khách QA Chat'}).click();await waitBubble(page,'Xin chào');}else await waitBubble(page,'Xin chào');
   const size=await page.evaluate(()=>({viewport:innerWidth,document:document.documentElement.scrollWidth}));assert.ok(size.document<=width+1,role+' overflow '+width);evidence.pages.push({role,width,...size});
   if(width===390||width===1440)await page.screenshot({path:path.join(out,role+'-'+width+'.png'),fullPage:true});
  }
  record('Customer/admin responsive pages at 320, 390, 768, 1440 pixels');
  assert.deepEqual(evidence.errors,[]);evidence.result='PASS';
 }catch(e){evidence.result='FAIL';evidence.failure=e.message;if(last)await last.screenshot({path:path.join(out,'failure.png'),fullPage:true}).catch(()=>{});throw e;}
 finally{fs.writeFileSync(path.join(out,'result.json'),JSON.stringify(evidence,null,2)+'\n');await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
