const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const base = process.env.QA_BASE_URL || 'http://localhost:8080';
const output = path.resolve(__dirname, '../qa/storefront-20261002');
fs.mkdirSync(output, { recursive: true });
const evidence = { base, checkedAt: new Date().toISOString(), pages: [], flows: [], errors: [] };
(async () => {
  const browser = await chromium.launch({ headless: true, executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe' });
  const context = await browser.newContext({ viewport: { width: 1440, height: 1080 }, locale: 'vi-VN' });
  const page = await context.newPage();
  page.on('pageerror', error => evidence.errors.push(error.message));
  const visit = async url => { const response = await page.goto(base + url); assert.equal(response.status(), 200, url); await page.waitForLoadState('networkidle'); return response; };
  const layout = async (url, width) => {
    await page.setViewportSize({ width, height: 1000 }); await visit(url);
    const dimensions = await page.evaluate(() => ({ viewport: innerWidth, document: document.documentElement.scrollWidth }));
    assert.ok(dimensions.document <= dimensions.viewport + 1, url + ' overflows at ' + width + ': ' + JSON.stringify(dimensions));
    evidence.pages.push({ url, width, status: 200, ...dimensions });
  };
  const login = async (email, password) => {
    await visit('/login'); await page.locator('[name=username]').fill(email); await page.locator('[name=password]').fill(password);
    await Promise.all([page.waitForURL(/\/(products|admin)$/), page.locator('form[action="/login"] button').click()]);
  };
  try {
    await visit('/');
    assert.equal(await page.locator('.retail-product-grid .product-card').count(), 12);
    await page.locator('[data-slide-pause]').click();
    await page.screenshot({ path: path.join(output, 'desktop.png'), fullPage: true });
    await page.screenshot({ path: path.join(output, 'desktop-first-screen.png') });
    const missingImages = await page.locator('main img').evaluateAll(images => images.filter(image => !image.complete || image.naturalWidth === 0).map(image => image.src));
    assert.deepEqual(missingImages, []);
    for (const width of [1440, 1024, 768, 390, 320]) {
      for (const url of ['/products', '/products/7', '/branches', '/warranty', '/order-lookup', '/login', '/register', '/compare?ids=7,8']) await layout(url, width);
      if (width === 390 || width === 320) {
        await visit('/products'); await page.screenshot({ path: path.join(output, 'mobile-' + width + '.png'), fullPage: true });
        assert.equal(await page.locator('.retail-product-grid').evaluate(grid => getComputedStyle(grid).gridTemplateColumns.split(' ').length), 2);
      }
    }
    evidence.flows.push({ name: 'Guest pages at 1440, 1024, 768, 390 and 320 pixels; all product and banner images loaded', result: 'PASS' });
    await page.setViewportSize({ width: 1440, height: 1000 }); await visit('/products');
    await page.locator('.slider-dot').nth(1).click(); assert.equal(await page.locator('#hero-work').getAttribute('aria-hidden'), 'false');
    await page.locator('[data-slide-step="-1"]').click(); assert.equal(await page.locator('#hero-phone').getAttribute('aria-hidden'), 'false');
    await page.locator('[data-slide-pause]').click(); assert.equal(await page.locator('[data-slide-pause]').getAttribute('aria-pressed'), 'true');
    await page.locator('.catalog-menu summary').click(); assert.equal(await page.locator('.catalog-menu').evaluate(menu => menu.open), true);
    await page.keyboard.press('Escape'); assert.equal(await page.locator('.catalog-menu').evaluate(menu => menu.open), false);
    await page.locator('[data-filter-toggle]').click(); assert.equal(await page.locator('#catalog-filters').isVisible(), false);
    await page.locator('[data-filter-toggle]').click();
    await page.locator('#catalog-filters [name=sort]').selectOption('price_asc');
    await Promise.all([page.waitForURL(/sort=price_asc/), page.locator('#catalog-filters button[type=submit]').click()]);
    const prices = await page.locator('.product-price').allTextContents();
    const numbers = prices.map(value => Number(value.replace(/\D/g, '')));
    assert.deepEqual(numbers, [...numbers].sort((a, b) => a - b));
    await page.locator('.category-chips a').nth(1).click();
    const selectedCategory = (await page.locator('.category-chips a.selected').innerText()).trim();
    const productCategories = await page.locator('.product-label').allTextContents(); assert.ok(productCategories.length > 0); assert.ok(productCategories.every(category => category === selectedCategory));
    await visit('/products'); await page.locator('#header-keyword').fill('Nova');
    await page.locator('[data-search-suggestions] a').first().waitFor();
    assert.ok((await page.locator('[data-search-suggestions]').innerText()).includes('Nova'));
    await Promise.all([page.waitForURL(/keyword=Nova/), page.locator('.header-search button').click()]);
    assert.ok((await page.locator('.product-info h3').allTextContents()).every(name => name.includes('Nova')));
    await visit('/products?keyword=zz-no-device-20261002'); assert.equal(await page.locator('.retail-empty').count(), 1);
    evidence.flows.push({ name: 'Banner tabs and arrows, pause, Escape menu, filter visibility, actual price sort, categories, autocomplete, search and empty state', result: 'PASS' });
    await visit('/products');
    await page.locator('[data-compare-id]').nth(0).click(); assert.equal(await page.locator('[data-compare-link]').getAttribute('aria-disabled'), 'true');
    await page.locator('[data-compare-id]').nth(1).click(); const comparisonHref = await page.locator('[data-compare-link]').getAttribute('href');
    await Promise.all([page.waitForURL(/\/compare\?ids=/), page.locator('[data-compare-link]').click()]);
    assert.equal(await page.locator('table thead th').count(), 3);
    await visit('/products'); assert.equal(await page.locator('[data-compare-id][aria-pressed=true]').count(), 2);
    await page.locator('[data-compare-id]').nth(2).click(); await page.locator('[data-compare-id]').nth(3).click();
    assert.ok((await page.locator('[data-compare-summary]').innerText()).includes('tối đa 3'));
    await page.locator('[data-compare-clear]').click(); assert.equal(await page.locator('[data-compare-tray]').isVisible(), false);
    evidence.flows.push({ name: 'Select 2 products, rendered comparison, session persistence, 3-item limit and clear selection', result: 'PASS', comparisonHref });
    await page.emulateMedia({ reducedMotion: 'reduce' }); await visit('/products'); assert.equal(await page.locator('[data-slide-pause]').getAttribute('aria-pressed'), 'true');
    await page.emulateMedia({ reducedMotion: 'no-preference' });
    evidence.flows.push({ name: 'Reduced motion disables automatic banner rotation', result: 'PASS' });
    await login('customer@techshop.test', 'DemoCustomer2026!');
    for (const width of [1440, 390, 320]) for (const url of ['/products', '/cart', '/checkout', '/orders', '/member', '/trade-in']) await layout(url, width);
    await page.locator('.account-menu summary').click(); assert.equal(await page.locator('.account-menu a[href="/orders"]').isVisible(), true);
    await page.locator('.account-menu button').click(); await page.waitForURL(/\/login\?logout/);
    await login('admin@techshop.test', 'DemoAdmin2026!');
    for (const width of [1440, 390, 320]) for (const url of ['/admin', '/admin/products', '/admin/commerce']) await layout(url, width);
    evidence.flows.push({ name: 'Customer cart, checkout, orders, member, trade-in and admin pages at desktop/mobile widths; account menu and logout', result: 'PASS' });
    assert.deepEqual(evidence.errors, []); evidence.result = 'PASS';
  } catch (error) { evidence.result = 'FAIL'; evidence.failure = error.stack; throw error; }
  finally { fs.writeFileSync(path.join(output, 'results.json'), JSON.stringify(evidence, null, 2)); await browser.close(); console.log(JSON.stringify({ result: evidence.result, pages: evidence.pages.length, flows: evidence.flows, errors: evidence.errors, failure: evidence.failure, output }, null, 2)); }
})().catch(error => { console.error(error.stack); process.exitCode = 1; });