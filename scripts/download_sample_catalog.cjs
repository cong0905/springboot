const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const root = path.resolve(__dirname, '..');
const output = path.join(root, 'src/main/resources/static/assets/products/real');
fs.mkdirSync(output, { recursive: true });
fs.mkdirSync(path.join(root, 'src/main/resources/demo'), { recursive: true });
fs.mkdirSync(path.join(root, 'qa/catalog-20261002'), { recursive: true });
// Explicit sample prices; these are not live prices scraped from the source.
const candidates = [
 ['IPHONE-15','PHONE','Apple',15990000,'128GB','iphone-15.html'],
 ['IPHONE-15-PLUS','PHONE','Apple',18990000,'128GB','iphone-15-plus.html'],
 ['IPHONE-16','PHONE','Apple',20990000,'128GB','iphone-16.html'],
 ['IPHONE-16-PLUS','PHONE','Apple',23990000,'128GB','iphone-16-plus.html'],
 ['IPHONE-16-PRO','PHONE','Apple',27990000,'128GB','iphone-16-pro.html'],
 ['IPHONE-16-PRO-MAX','PHONE','Apple',31990000,'256GB','iphone-16-pro-max.html'],
 ['GALAXY-S24-ULTRA','PHONE','Samsung',24990000,'256GB','samsung-galaxy-s24-ultra.html'],
 ['GALAXY-S24-PLUS','PHONE','Samsung',18990000,'256GB','samsung-galaxy-s24-plus.html'],
 ['GALAXY-A55','PHONE','Samsung',8990000,'128GB','samsung-galaxy-a55.html'],
 ['XIAOMI-14','PHONE','Xiaomi',14990000,'256GB','xiaomi-14.html'],
 ['MACBOOK-AIR-M3-13','LAPTOP','Apple',25990000,'16GB / 256GB','macbook-air-m3-13-inch-2024-16gb-256gb-10gpu.html'],
 ['MACBOOK-AIR-M3-15','LAPTOP','Apple',28990000,'16GB / 256GB','macbook-air-m3-15-inch-2024-16gb-256gb.html'],
 ['MACBOOK-AIR-M2','LAPTOP','Apple',21990000,'8GB / 256GB','macbook-air-m2-2022.html'],
 ['MACBOOK-PRO-M3','LAPTOP','Apple',37990000,'8GB / 512GB','macbook-pro-14-inch-m3-2023-8gb-512gb.html'],
 ['IPAD-10','TABLET','Apple',9490000,'64GB','ipad-10-9-inch-2022.html'],
 ['IPAD-AIR-M2','TABLET','Apple',15990000,'128GB','ipad-air-6-m2-11-inch.html'],
 ['IPAD-PRO-M4','TABLET','Apple',26990000,'256GB','ipad-pro-m4-11-inch.html'],
 ['GALAXY-TAB-S9','TABLET','Samsung',16990000,'128GB','samsung-galaxy-tab-s9.html'],
 ['APPLE-WATCH-S9','WATCH','Apple',9490000,'41mm','apple-watch-series-9-41mm.html'],
 ['APPLE-WATCH-SE','WATCH','Apple',5990000,'40mm','apple-watch-se-2022-40mm.html'],
 ['GALAXY-WATCH6','WATCH','Samsung',5490000,'40mm','samsung-galaxy-watch-6.html'],
 ['AIRPODS-PRO-2','AUDIO','Apple',5490000,'USB-C','apple-airpods-pro-2-usb-c.html'],
 ['AIRPODS-4','AUDIO','Apple',3490000,'Tiêu chuẩn','apple-airpods-4.html'],
 ['SONY-WH-1000XM5','AUDIO','Sony',6990000,'Bluetooth','tai-nghe-chup-tai-sony-wh-1000xm5.html'],
 ['JBL-FLIP6','AUDIO','JBL',2490000,'Bluetooth','loa-bluetooth-jbl-flip-6.html'],
 ['JBL-CHARGE5','AUDIO','JBL',3290000,'Bluetooth','loa-bluetooth-jbl-charge-5.html'],
 ['GALAXY-BUDS2-PRO','AUDIO','Samsung',2990000,'Bluetooth','samsung-galaxy-buds-2-pro.html'],
 ['APPLE-20W','GEAR','Apple',490000,'20W / USB-C','cu-sac-nhanh-iphone-20w-pd-type-c.html'],
 ['LOGITECH-MX-MASTER3S','GEAR','Logitech',2190000,'Không dây','chuot-khong-day-logitech-mx-master-3s.html'],
 ['LOGITECH-K380','GEAR','Logitech',690000,'Bluetooth','ban-phim-khong-day-logitech-k380.html'],
 ['SAMSUNG-25W','GEAR','Samsung',390000,'25W / USB-C','cu-sac-nhanh-samsung-25w-ep-t2510nbegww.html'],
 ['ANKER-737','GEAR','Anker',2890000,'24000mAh','pin-sac-du-phong-anker-737-powercore-24000-140w-a1289.html']
];
const clean = text => String(text || '').replace(/<[^>]*>/g, ' ').replace(/&amp;/g, '&').replace(/&quot;/g, '"').replace(/&nbsp;/g, ' ').replace(/\s+/g, ' ').trim();
const specNames = new Set(['Chipset','Loại CPU','Dung lượng RAM','Bộ nhớ trong','Ổ cứng','Kích thước màn hình','Công nghệ màn hình','Pin','Thời lượng pin','Cổng giao tiếp','Công suất','Kết nối','Bluetooth','Dung lượng pin','Loại kết nối','Công suất sạc','Tính năng','Kích thước mặt','Công nghệ âm thanh']);
const manifestPath = path.join(root, 'src/main/resources/demo/retail-catalog.json');
const results = process.argv.includes('--missing') && fs.existsSync(manifestPath) ? JSON.parse(fs.readFileSync(manifestPath, 'utf8')).products : [], failures = [];
const pending = candidates.filter(candidate => !results.some(product => product.sku === 'REAL-' + candidate[0]));
const fetchLimited = (url, timeout = 40000) => fetch(url, { signal: AbortSignal.timeout(timeout), headers: { 'User-Agent': 'Mozilla/5.0', 'Accept': '*/*' } });
async function downloadImage(url, basename) {
  let response = await fetchLimited(url, 25000);
  if (!response.ok) throw Error('Image HTTP ' + response.status + ': ' + url);
  const contentType = response.headers.get('content-type') || '';
  if (!contentType.startsWith('image/')) throw Error('Not an image: ' + contentType);
  const bytes = Buffer.from(await response.arrayBuffer());
  if (bytes.length < 1500) throw Error('Image too small: ' + bytes.length);
  const extension = contentType.includes('webp') ? '.webp' : contentType.includes('png') ? '.png' : '.jpg';
  fs.writeFileSync(path.join(output, basename + extension), bytes);
  return { assetPath: '/assets/products/real/' + basename + extension, sourceUrl: url, contentType, bytes: bytes.length, sha256: crypto.createHash('sha256').update(bytes).digest('hex') };
}
async function collect(candidate) {
  const [sku, category, brand, price, storage, slug] = candidate;
  const sourceUrl = 'https://cellphones.com.vn/' + slug;
  try {
    let html, response;
    for (let attempt = 0; attempt < 2; attempt++) {
      try { response = await fetchLimited(sourceUrl); html = await response.text(); break; } catch (error) { if (attempt === 1) throw error; }
    }
    if (!response.ok) throw Error('Page HTTP ' + response.status);
    let product;
    for (const match of html.matchAll(/<script[^>]*type="application\/ld\+json"[^>]*>([\s\S]*?)<\/script>/g)) {
      try { const data = JSON.parse(match[1]); if (data['@type'] === 'Product') product = data; } catch (error) { }
    }
    if (!product || !product.image || !product.name) throw Error('No product structured data');
    const name = clean(product.name).replace(/\s*\|.*$/, '').trim();
    const mainUrl = typeof product.image === 'string' ? product.image : product.image[0];
    if (!mainUrl.includes('/media/catalog/product/')) throw Error('Non-product image');
    const assetStem = sku.toLowerCase();
    const photo = await downloadImage(mainUrl.replace('/200x/', '/600x/'), assetStem + '-1');
    const entry = { sku: 'REAL-' + sku, name, category, brand, samplePrice: price, storage, sourceUrl, images: [photo], specifications: (product.additionalProperty || []).filter(spec => specNames.has(clean(spec.name)) && clean(spec.value).length <= 200).slice(0, 7).map(spec => ({ name: clean(spec.name), value: clean(spec.value) })) };
    // Only take an additional angle from the same file stem, never related-product images.
    const fileName = new URL(mainUrl).pathname.split('/').pop().replace(/\.[^.]+$/, '');
    const stem = fileName.replace(/[-_]\d+$/, '');
    const extraUrls = [...new Set([...html.matchAll(/https:\/\/cdn2?\.cellphones\.com\.vn\/x\/media\/catalog\/product\/[^"\s<>\\]+\.(?:png|jpg|webp)/g)].map(match => match[0]))];
    const extraUrl = extraUrls.find(url => !url.endsWith(new URL(mainUrl).pathname.split('/').pop()) && new URL(url).pathname.split('/').pop().startsWith(stem) && stem.length > 6);
    if (extraUrl && ['PHONE','LAPTOP','TABLET'].includes(category)) {
      try { entry.images.push(await downloadImage(extraUrl.replace('/x/', '/600x/'), assetStem + '-2')); } catch (error) { console.log('Optional angle skipped:', sku, error.message); }
    }
    results.push(entry); console.log('OK', sku, '=>', name, entry.images.length + ' photo(s)');
  } catch (error) { failures.push({ sku, sourceUrl, error: error.message }); console.log('FAIL', sku, error.message); }
}
(async () => {
  let cursor = 0;
  await Promise.all(Array.from({ length: 4 }, async () => { while (cursor < pending.length) await collect(pending[cursor++]); }));
  const order = new Map(candidates.map((candidate, index) => ['REAL-' + candidate[0], index]));
  results.sort((a, b) => order.get(a.sku) - order.get(b.sku));
  const catalog = { note: 'Tên và ảnh sản phẩm thật; giá, tồn kho và serial là dữ liệu mẫu TechShop.', capturedAt: new Date().toISOString(), products: results };
  fs.writeFileSync(path.join(root, 'src/main/resources/demo/retail-catalog.json'), JSON.stringify(catalog, null, 2));
  fs.writeFileSync(path.join(root, 'qa/catalog-20261002/download-results.json'), JSON.stringify({ products: results.length, photos: results.reduce((sum, entry) => sum + entry.images.length, 0), failures }, null, 2));
  console.log(JSON.stringify({ products: results.length, photos: results.reduce((sum, entry) => sum + entry.images.length, 0), failures }, null, 2));
})();