const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require('C:/Users/nguye/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const base='http://localhost:8080',out=path.resolve(__dirname,'../qa/techzone-step01');fs.mkdirSync(out,{recursive:true});
const report={base,checkedAt:new Date().toISOString(),screens:[],flows:[],errors:[]};let browser;
(async()=>{
 browser=await chromium.launch({headless:true,executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe'});
 const context=await browser.newContext({viewport:{width:1440,height:1000},locale:'vi-VN'});context.setDefaultNavigationTimeout(20000);
 const page=await context.newPage();page.on('pageerror',e=>report.errors.push(e.message));
 const visit=async route=>{const r=await page.goto(base+route);assert.equal(r.status(),200,route);await page.waitForLoadState('networkidle');};
 const json=async route=>{const r=await context.request.get(base+route);assert.equal(r.status(),200,route);return r.json()};
 const catalogue=await json('/api/v1/products?size=50');assert.equal(catalogue.totalElements,40);
 for(const width of [1440,1024,768,390,320]){await page.setViewportSize({width,height:1000});await visit('/products');
  assert.ok((await page.locator('.brand').innerText()).includes('TechZone'));assert.equal(await page.locator('.service-nav a[href*="categoryId="]').count(),6);assert.equal(await page.locator('.tz-promo').count(),2);assert.equal(await page.locator('[data-quick-add]').count(),12);
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true,'home overflow '+width);
  await page.locator('main img').evaluateAll(xs=>Promise.all(xs.map(x=>{x.loading='eager';return x.decode()})));
  await page.locator('[data-slide-pause]').click();await page.screenshot({path:path.join(out,'home-'+width+'.png'),fullPage:true});if(width===1440)await page.screenshot({path:path.join(out,'home-first-screen.png')});
  report.screens.push({route:'/products',width,result:'PASS'});
  for(const route of ['/products?keyword=Galaxy%20S24','/products/7','/compare?ids=7,8','/branches','/warranty','/login','/register','/order-lookup']){
   await visit(route);assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true,route+' overflow '+width);assert.equal(await page.locator('.service-nav a[href*="categoryId="]').count(),6);report.screens.push({route,width,result:'PASS'});
  }
 }
 report.flows.push('Five widths: home and eight other public pages, database-backed navigation and local images');
 await page.setViewportSize({width:1440,height:1000});await visit('/products');await page.locator('.slider-dot').nth(1).click();assert.equal(await page.locator('#hero-work').getAttribute('aria-hidden'),'false');await page.locator('[data-slide-step="-1"]').click();assert.equal(await page.locator('#hero-phone').getAttribute('aria-hidden'),'false');
 await page.emulateMedia({reducedMotion:'reduce'});await visit('/products');assert.equal(await page.locator('[data-slide-pause]').getAttribute('aria-pressed'),'true');await page.emulateMedia({reducedMotion:'no-preference'});
 await page.locator('#hero-phone .hero-cta').click();await page.waitForURL(/keyword=Galaxy/);const names=await page.locator('.product-info h3').allTextContents();assert.ok(names.length>0&&names.every(x=>x.includes('Galaxy S24')));
 await visit('/products');await page.locator('[data-quick-add]').first().click();await page.waitForURL(/\/login$/);
 await page.locator('[name=username]').fill('customer@techshop.test');await page.locator('[name=password]').fill('DemoCustomer2026!');await Promise.all([page.waitForURL(/\/products$/),page.locator('form[action="/login"] button').click()]);
 const before=await json('/api/v1/cart');const countBefore=before.items.reduce((n,x)=>n+x.quantity,0);
 const first=page.locator('[data-quick-add]').first();const firstId=Number(await first.getAttribute('data-quick-add'));
 await first.click();await page.locator('[data-buy-dialog]').waitFor({state:'visible'});assert.equal(await page.locator('[data-buy-variant] option').count(),1);const chosen=Number(await page.locator('[data-buy-variant]').inputValue());
 await Promise.all([page.waitForResponse(r=>r.url().endsWith('/api/v1/cart/items')&&r.request().method()==='POST'),page.locator('[data-buy-confirm]').click()]);await page.locator('[data-buy-dialog]').waitFor({state:'hidden'});
 const after=await json('/api/v1/cart');assert.ok(after.items.some(x=>x.productId===firstId&&x.variantId===chosen&&x.quantity===1));assert.equal(Number(await page.locator('[data-header-cart-count]').innerText()),countBefore+1);
 await visit('/products?keyword=Nova');await page.locator('[data-quick-add]').first().click();await page.locator('[data-buy-dialog]').waitFor({state:'visible'});assert.equal(await page.locator('[data-buy-variant] option').count(),2);
 await page.locator('[data-buy-variant]').selectOption({index:1});const alternative=Number(await page.locator('[data-buy-variant]').inputValue());await Promise.all([page.waitForResponse(r=>r.url().endsWith('/api/v1/cart/items')&&r.request().method()==='POST'),page.locator('[data-buy-confirm]').click()]);await page.locator('[data-buy-dialog]').waitFor({state:'hidden'});
 assert.ok((await json('/api/v1/cart')).items.some(x=>x.variantId===alternative));await page.reload();assert.equal(Number(await page.locator('[data-header-cart-count]').innerText()),countBefore+2);
 // Every displayed original-price entry is a real variant original price matching the displayed sale price.
 await visit('/products?keyword=Nova');const variants=await json('/api/v1/products/7/variants');assert.ok((await page.locator('.tz-original-price').innerText()).includes(new Intl.NumberFormat('vi-VN').format(Math.max(...variants.filter(v=>v.price===catalogue.content.find(p=>p.id===7).price).map(v=>v.originalPrice)))));
 await visit('/products?keyword=Tai%20nghe');const stats=(await json('/api/v1/products/1/reviews')).stats;
 if(stats.totalReviews>0){const productCard=page.locator('.product-card').filter({has:page.locator('[data-quick-add="1"]')});assert.ok((await productCard.locator('.tz-card-rating').innerText()).includes(String(stats.totalReviews)));}
 report.flows.push('Slider, reduced motion, hero search link, guest login, single/multiple variant quick-add, persisted cart badge, actual prices and review counts');
 for(const width of [1440,390,320]){await page.setViewportSize({width,height:1000});for(const route of ['/cart','/checkout','/orders','/member']){await visit(route);assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true,route+' '+width);report.screens.push({route,width,result:'PASS'});}}
 await page.locator('.account-menu summary').click();await Promise.all([page.waitForURL(/\/login\?logout/),page.locator('.account-menu button').click()]);
 await page.locator('[name=username]').fill('admin@techshop.test');await page.locator('[name=password]').fill('DemoAdmin2026!');await Promise.all([page.waitForURL(/\/admin$/),page.locator('form[action="/login"] button').click()]);for(const width of [1440,390,320]){await page.setViewportSize({width,height:1000});for(const route of ['/admin','/admin/products','/admin/commerce','/products']){await visit(route);assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),true,route+' '+width);report.screens.push({route,width,result:'PASS'});if(route==='/products')assert.equal(await page.locator('[data-quick-add]:enabled').count(),0);}}
 report.flows.push('Customer and admin shared shell; customer checkout preserved, admin purchase disabled');assert.deepEqual(report.errors,[]);report.result='PASS';
})().catch(e=>{report.result='FAIL';report.failure=e.stack;console.error(e)}).finally(async()=>{fs.writeFileSync(path.join(out,'results.json'),JSON.stringify(report,null,2));if(browser)await browser.close();console.log(JSON.stringify({result:report.result,screens:report.screens.length,flows:report.flows,errors:report.errors,failure:report.failure},null,2));if(report.result==='FAIL')process.exitCode=1});