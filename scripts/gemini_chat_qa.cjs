/* UI AI responses are explicitly mocked. Real backend chat without a Gemini key is checked first. */
'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const base=process.env.QA_BASE_URL||'http://localhost:8096',out=path.resolve(__dirname,'../qa/gemini/browser');fs.mkdirSync(out,{recursive:true});
const result={base,checkedAt:new Date().toISOString(),provider:'[Giả lập] Browser routes; no Google API calls',checks:[],pages:[],errors:[]};
const record=(name,mode)=>{result.checks.push({name,mode,result:'PASS'});console.log('PASS '+name);};
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROME_PATH||undefined});let page;
 try{
  const context=await browser.newContext();page=await context.newPage();page.on('pageerror',e=>result.errors.push(e.message));
  const csrf=await context.request.get(base+'/api/v1/csrf').then(r=>r.json());const email='gemini-ui-'+Date.now()+'@test.local';
  assert.equal((await context.request.post(base+'/api/v1/auth/register',{headers:{[csrf.headerName]:csrf.token},data:{fullName:'Khách QA Gemini',email,password:'GeminiBrowser2026!'}})).status(),201);
  await page.goto(base+'/login?redirect=/chat');await page.locator('[name=username]').fill(email);await page.locator('[name=password]').fill('GeminiBrowser2026!');await Promise.all([page.waitForURL('**/chat'),page.locator('form[action="/login"] button').click()]);
  await page.locator('[data-ai-open]').filter({hasText:'chưa được cấu hình'}).waitFor();assert.equal(await page.locator('[data-ai-open]').isDisabled(),true);assert.equal(await page.locator('[data-human-handoff]').isEnabled(),true);
  await page.locator('[data-human-handoff]').click();await page.locator('[data-support-status]').filter({hasText:'chuyển sang nhân viên'}).waitFor();record('Missing key disables AI and real handoff endpoint remains usable','Real backend');
  let support={available:true,supportMode:'ADMIN',handoffStatus:'NONE',aiStatus:'IDLE'},messages=[],started=false;
  await page.route('**/api/v1/chat/**',async route=>{
   const req=route.request(),url=new URL(req.url()),action=url.pathname;
   let body={};if(action.endsWith('/ai/start')){assert.equal(req.postDataJSON().consent,true);assert.ok(req.headers()['x-csrf-token']);support={...support,supportMode:'AI',handoffStatus:'NONE',aiStatus:'IDLE'};started=true;body=support;}
   else if(action.endsWith('/handoff')){assert.ok(req.headers()['x-csrf-token']);support={...support,supportMode:'ADMIN',handoffStatus:'WAITING',aiStatus:'IDLE'};body=support;}
   else if(action.endsWith('/messages')&&req.method()==='POST'){
    const input=req.postDataJSON();messages.push({id:1,senderRole:'CUSTOMER',senderName:'Khách',content:input.content,createdAt:new Date().toISOString(),products:[]});
    messages.push({id:2,senderRole:'AI',senderName:'Trợ lý AI TechZone',content:'[Giả lập] <img src=x onerror="window.aiUnsafe=true"> Mời bạn xem thiết bị dưới đây.',createdAt:new Date().toISOString(),inReplyToId:1,products:[{id:1,name:'Thiết bị tư vấn mẫu',price:5000000,currency:'VND',available:4,url:'https://invalid.test/unsafe',image:'/assets/products/device.svg',retrievedAt:new Date().toISOString()}]});body=messages[0];
   }else if(action.endsWith('/messages')){const after=Number(url.searchParams.get('afterId')||0);body={messages:messages.filter(m=>m.id>after),hasMore:false,customerReadId:0,adminReadId:0,support};}
   else if(action.endsWith('/read')){await route.fulfill({status:204});return;}
   else if(action.endsWith('/unread'))body={unreadCount:0};
   await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(body)});
  });
  await page.reload();await page.locator('[data-ai-open]').filter({hasText:'Tư vấn bằng AI'}).waitFor();await page.locator('[data-ai-open]').click();assert.equal(await page.locator('[data-ai-start]').isDisabled(),true);await page.locator('[data-ai-agree]').check();await page.locator('[data-ai-start]').click();await page.locator('[data-support-status]').filter({hasText:'Trợ lý AI đang hỗ trợ'}).waitFor();assert.equal(started,true);assert.equal(await page.locator('[data-ai-consent]').isVisible(),false);record('Consent is required before enabling AI','[Giả lập] UI');
  await page.locator('[name=content]').fill('Tư vấn điện thoại');await page.locator('[data-chat-form] button[type=submit]').click();await page.locator('.chat-message-ai').waitFor();assert.equal(await page.locator('.chat-message-ai .chat-message-author').innerText(),'Trợ lý AI · Gemini');assert.equal(await page.locator('.chat-product-card').getAttribute('href'),'/products/1');assert.ok((await page.locator('.chat-product-price').innerText()).includes('5.000.000'));assert.equal(await page.evaluate(()=>window.aiUnsafe),undefined);assert.equal(await page.locator('.chat-bubble img').count(),0);record('AI label, deterministic product link/price and safe text rendering','[Giả lập] UI');
  for(const width of [320,390,768,1440]){await page.setViewportSize({width,height:1000});const size=await page.evaluate(()=>({viewport:innerWidth,document:document.documentElement.scrollWidth}));assert.ok(size.document<=width+1,'Overflow '+width);result.pages.push({width,...size});await page.evaluate(()=>window.scrollTo({top:0,behavior:'instant'}));if(width===390||width===1440)await page.screenshot({path:path.join(out,'ai-'+width+'.png'),fullPage:true});}
  record('AI product cards responsive at 320/390/768/1440 pixels','[Giả lập] UI');
  await page.locator('[data-human-handoff]').click();await page.locator('[data-support-status]').filter({hasText:'chuyển sang nhân viên'}).waitFor();assert.equal(await page.locator('[data-human-handoff]').isDisabled(),true);record('Customer handoff updates support status','[Giả lập] UI');
  assert.deepEqual(result.errors,[]);result.result='PASS';
 }catch(e){result.result='FAIL';result.failure=e.message;if(page)await page.screenshot({path:path.join(out,'failure.png'),fullPage:true}).catch(()=>{});throw e;}
 finally{fs.writeFileSync(path.join(out,'result.json'),JSON.stringify(result,null,2)+'\n');await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
