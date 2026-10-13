const fs=require('node:fs');
const path=require('node:path');
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const assert=require('node:assert/strict');
const base=process.env.QA_BASE_URL||'http://localhost:8091';
const out=path.resolve(__dirname,'../qa');fs.mkdirSync(out,{recursive:true});
const evidence={base,checkedAt:new Date().toISOString(),pages:[],errors:[],flows:[]};
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROME_PATH||'C:/Program Files/Google/Chrome/Application/chrome.exe'});
 try {
  const customer=await browser.newContext({viewport:{width:1440,height:1000},locale:'vi-VN'});
  const page=await customer.newPage();page.on('pageerror',e=>evidence.errors.push(e.message));
  const login=async(p,email,password)=>{await p.goto(base+'/login');await p.locator('[name=username]').fill(email);await p.locator('[name=password]').fill(password);await Promise.all([p.waitForURL(/\/(products|admin)$/),p.locator('form[action="/login"] button').click()]);};
  const guest=await browser.newContext({viewport:{width:390,height:1000},locale:'vi-VN'});const guestPage=await guest.newPage();guestPage.on('pageerror',e=>evidence.errors.push(e.message));
  await guestPage.goto(base+'/products/7');await guestPage.locator('[data-variant]').selectOption({index:1});
  await guestPage.waitForFunction(()=>document.querySelector('[data-stores]')?.textContent.includes('Thái Hà'));
  await guestPage.locator('[name=provinceCode]').selectOption('HN');await guestPage.locator('[name=districtCode]').selectOption('DONG-DA');
  await guestPage.waitForFunction(()=>document.querySelector('[data-stores]')?.textContent.includes('Thái Hà')&&!document.querySelector('[data-stores]').textContent.includes('Cầu Giấy'));
  assert.equal(await guestPage.locator('#product-cart').count(),0);
  await guestPage.goto(base+'/branches');await guestPage.locator('[name=districtCode]').selectOption('CAU-GIAY');
  assert.equal(await guestPage.locator('[data-branch-card]:visible').count(),1);
  assert.ok((await guestPage.locator('[data-branch-card]:visible').innerText()).includes('Cầu Giấy'));
  evidence.flows.push({name:'Guest variant album + province/district stock filters + branch directory',result:'PASS'});await guest.close();
  await login(page,'customer@techshop.test','DemoCustomer2026!');
  for(const width of [1440,390,320]) {
   await page.setViewportSize({width,height:1000});
   for(const url of ['/products','/products/7','/branches','/compare?ids=7,8','/member','/trade-in','/warranty']) {
    const response=await page.goto(base+url);assert.equal(response.status(),200,url);
    await page.waitForLoadState('networkidle');
    const dims=await page.evaluate(()=>({viewport:innerWidth,document:document.documentElement.scrollWidth}));
    assert.ok(dims.document<=dims.viewport+1,`${url} overflows at ${width}: ${JSON.stringify(dims)}`);
    evidence.pages.push({url,width,status:response.status(),...dims});
    if(width===1440&&url==='/products')await page.screenshot({path:path.join(out,'browser-storefront-desktop.png'),fullPage:true});
    if(width===390&&url==='/products/7')await page.screenshot({path:path.join(out,'browser-product-mobile.png'),fullPage:true});
   }
  }
  await page.setViewportSize({width:1440,height:1000});
  const productVariants=await customer.request.get(base+'/api/v1/products/7/variants').then(r=>r.json());
  await page.goto(base+'/products/7');await page.locator('[name=variantId]').selectOption(String(productVariants[0].id));
  await page.waitForFunction(()=>document.querySelector('#main-product-img')?.getAttribute('src')?.includes('phone-black'));
  await page.locator('[name=variantId]').selectOption(String(productVariants[1].id));
  await page.waitForFunction(()=>document.querySelector('#main-product-img')?.getAttribute('src')?.includes('phone-silver'));
  await page.locator('[name=variantId]').selectOption(String(productVariants[0].id));
  evidence.flows.push({name:'Variant changes album to its own illustration',result:'PASS'});
  await Promise.all([page.waitForURL('**/cart'),page.locator('form[data-api="/api/v1/cart/items"] button').click()]);
  await page.goto(base+'/checkout');await page.locator('[name=fulfillmentType]').selectOption('CLICK_AND_COLLECT');await page.locator('[name=pickupBranchId]').selectOption('1');
  await page.locator('[data-refresh-preview]').click();await page.waitForFunction(()=>document.querySelector('[data-checkout]').dataset.previewReady==='true');
  let releasePreview,startedPreview,finishedPreview;const previewStarted=new Promise(resolve=>{startedPreview=resolve;});const previewGate=new Promise(resolve=>{releasePreview=resolve;});const previewFinished=new Promise(resolve=>{finishedPreview=resolve;});
  const delayedPreview=async route=>{try {const response=await route.fetch();startedPreview();await previewGate;await route.fulfill({response});} catch(error) {if(!/already handled|aborted/i.test(error.message))throw error;} finally {finishedPreview();}};
  await page.route('**/api/v1/orders/preview',delayedPreview);await page.locator('[data-refresh-preview]').click();await previewStarted;
  await page.locator('[name=fulfillmentType]').selectOption('HOME_DELIVERY_STANDARD');releasePreview();await previewFinished;await page.unroute('**/api/v1/orders/preview',delayedPreview);
  await page.waitForLoadState('networkidle');assert.equal(await page.locator('[data-place-order]').isDisabled(),true);
  assert.equal(await page.locator('[name=pickupBranchId]').isDisabled(),true);
  await page.locator('[name=fulfillmentType]').selectOption('CLICK_AND_COLLECT');
  evidence.flows.push({name:'Delayed preview fault injection cannot re-enable checkout after selections change',result:'PASS'});
  await page.locator('[name=phone]').fill('0900000000');await page.locator('[name=address]').fill('123 Thái Hà Hà Nội - dữ liệu kiểm thử');
  await page.locator('[data-refresh-preview]').click();await page.locator('[data-place-order]').waitFor({state:'visible'});
  await page.waitForFunction(()=>!document.querySelector('[data-place-order]').disabled);
  const priceConflict=route=>route.fulfill({status:409,contentType:'application/json',body:JSON.stringify({code:'PRICE_CHANGED',message:'Giá đã đổi (QA fault injection)',fieldErrors:[]})});
  await page.route('**/api/v1/orders',priceConflict);await page.locator('[data-place-order]').click();
  await page.waitForFunction(()=>document.querySelector('[data-checkout]').dataset.busy==='false'&&document.querySelector('[data-checkout]').dataset.previewReady==='false');
  assert.equal(await page.locator('[data-place-order]').isDisabled(),true);await page.unroute('**/api/v1/orders',priceConflict);
  await page.locator('[data-refresh-preview]').click();await page.waitForFunction(()=>!document.querySelector('[data-place-order]').disabled);
  evidence.flows.push({name:'409 price conflict fault injection requires a new preview before retry',result:'PASS'});
  await Promise.all([page.waitForURL(/\/orders\/\d+$/),page.locator('[data-place-order]').click()]);
  const id=page.url().split('/').pop();const pickupCode=(await page.locator('code').innerText()).trim();assert.ok(pickupCode.length===36);
  await page.waitForFunction(()=>document.querySelector('.pickup-qr img')?.naturalWidth===256);
  assert.equal((await customer.request.get(base+'/api/v1/orders/'+id+'/pickup-qr.png')).status(),200);
  evidence.flows.push({name:'Owner pickup QR displayed from authenticated PNG endpoint',result:'PASS'});
  await page.screenshot({path:path.join(out,'browser-order-pickup.png'),fullPage:true});
  const admin=await browser.newContext({viewport:{width:1440,height:1000},locale:'vi-VN'});const adminPage=await admin.newPage();adminPage.on('pageerror',e=>evidence.errors.push(e.message));
  await login(adminPage,'admin@techshop.test','DemoAdmin2026!');
  const csrf=await admin.request.get(base+'/api/v1/csrf').then(r=>r.json());
  const png=Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl2dTcAAAAASUVORK5CYII=','base64');
  const upload=await admin.request.post(base+'/api/v1/admin/products/7/images',{headers:{[csrf.headerName]:csrf.token},multipart:{file:{name:'jar-check.png',mimeType:'image/png',buffer:png},sortOrder:'9'}});
  assert.equal(upload.status(),201);const uploaded=await upload.json();
  const asset=await customer.request.get(base+uploaded.assetPath);assert.equal(asset.status(),200);assert.deepEqual(await asset.body(),png);
  assert.equal((await admin.request.delete(base+'/api/v1/admin/products/7/images/'+uploaded.id,{headers:{[csrf.headerName]:csrf.token}})).status(),204);
  assert.equal((await customer.request.get(base+uploaded.assetPath)).status(),404);
  evidence.flows.push({name:'Packaged JAR multipart upload → public image GET → delete removes file',result:'PASS'});
  await adminPage.goto(base+'/admin/orders/'+id);const pickupForm=adminPage.locator('form[data-api$="/pickup"]');await pickupForm.locator('[name=pickupCode]').fill(pickupCode);
  await Promise.all([adminPage.waitForEvent('load'),pickupForm.locator('button:not([type])').click()]);
  const order=await admin.request.get(base+'/api/v1/admin/orders/'+id).then(r=>r.json());assert.equal(order.status,'DELIVERED');assert.equal(order.items[0].imeiNumbers.length,1);
  assert.equal((await customer.request.get(base+'/api/v1/orders/'+id+'/pickup-qr.png')).status(),404);
  await page.goto(base+'/member');assert.ok((await page.innerText('main')).includes('S_MEM'));
  evidence.flows.push({name:'Browser Click & Collect → admin pickup → IMEI sold → S_MEM + warranty',orderId:id,status:order.status});
  for(const width of [1440,390,320]) {await adminPage.setViewportSize({width,height:1000});for(const url of ['/admin','/admin/commerce','/admin/products/7/edit']) {const res=await adminPage.goto(base+url);assert.equal(res.status(),200,url);const dims=await adminPage.evaluate(()=>({viewport:innerWidth,document:document.documentElement.scrollWidth}));assert.ok(dims.document<=dims.viewport+1,`${url} overflows at ${width}`);evidence.pages.push({url,width,status:res.status(),...dims});}}
  await adminPage.setViewportSize({width:1440,height:1000});await adminPage.goto(base+'/admin/commerce');await adminPage.screenshot({path:path.join(out,'browser-admin-imei.png'),fullPage:true});
  assert.deepEqual(evidence.errors,[]);evidence.result='PASS';
 } catch(e) {evidence.result='FAIL';evidence.failure=e.stack;throw e;}
 finally {fs.writeFileSync(path.join(out,'browser-results.json'),JSON.stringify(evidence,null,2));await browser.close();console.log(JSON.stringify(evidence,null,2));}
})().catch(e=>{console.error(e.stack);process.exitCode=1;});
