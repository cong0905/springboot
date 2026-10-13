'use strict';

const guestPrefix=location.pathname.startsWith('/guest/')?'/guest':'';
const token = () => document.querySelector('meta[name="csrf-token"]').content;
const money = value => new Intl.NumberFormat('vi-VN').format(value) + ' ₫';
function localizeErrorMessage(result, status) {
  if (status === 403) return 'Phiên thao tác hết hạn hoặc không có quyền thực hiện. Vui lòng tải lại trang.';
  if (!result) return 'Chưa thể xử lý yêu cầu. Vui lòng thử lại sau.';
  let msg = result.message || '';
  if (msg.includes('BUSINESS_RULE_VIOLATION') || msg.includes('INSUFFICIENT_STOCK')) {
    msg = 'Sản phẩm tạm thời không đủ số lượng tồn kho khả dụng để đặt hàng.';
  } else if (msg.includes('PRODUCT_UNAVAILABLE')) {
    msg = 'Sản phẩm đang tạm ngừng kinh doanh hoặc chưa khả dụng.';
  } else if (msg.includes('CART_LIMIT')) {
    msg = 'Giỏ hàng đã đạt giới hạn tối đa 20 sản phẩm.';
  } else if (msg.includes('CART_CHANGED')) {
    msg = 'Giỏ hàng đã có thay đổi từ phiên khác. Vui lòng kiểm tra lại.';
  }
  const fields = (result.fieldErrors || []).map(e => (e.field ? e.field + ': ' : '') + e.message).join(' · ');
  return msg + (fields ? (msg ? ' · ' : '') + fields : '');
}

for (const form of document.querySelectorAll('form[data-api]')) {
  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (form.dataset.busy === 'true') return;
    const message = form.querySelector('.form-message');
    const button = form.querySelector('button[type="submit"],button:not([type])');
    if(form.hasAttribute('data-checkout')&&form.dataset.previewReady!=='true') {
      message.textContent='Vui lòng tính lại giá trước khi đặt hàng.';return;
    }
    const body = {};
    for (const field of form.elements) {
      if (!field.name || field.disabled) continue;
      body[field.name] = field.hasAttribute('data-list') ? field.value.split(/[,\n\r]+/).map(s=>s.trim()).filter(Boolean) : field.type === 'checkbox' ? field.checked : field.hasAttribute('data-number') ? (field.value === '' ? null : Number(field.value)) : field.value;
    }
    if(form.hasAttribute('data-checkout')) {body.options=checkoutOptions(form);for(const key of optionNames)delete body[key];}
    if(form.hasAttribute('data-checkout'))body.selectedItemIds=selectedCheckoutIds;
    if ('note' in body && !body.note.trim()) body.note = null;
    for(const field of form.elements) if(field.name && field.hasAttribute('data-null-empty') && body[field.name]==='') body[field.name]=null;
    form.dataset.busy = 'true'; button.disabled = true; message.textContent = '';
    try {
      const method = form.dataset.method || 'POST';
      let endpoint=form.dataset.api;for(const key of Object.keys(body)){if(endpoint.includes('{'+key+'}')){endpoint=endpoint.replace('{'+key+'}',encodeURIComponent(body[key]));delete body[key];}}
      const response = await fetch(endpoint, {method, credentials:'same-origin', headers:{'Content-Type':'application/json','Accept':'application/json','X-CSRF-TOKEN':token()}, body:method === 'DELETE' ? undefined : JSON.stringify(body)});
      const result = response.status === 204 ? null : await response.json();
      if (!response.ok) {
        if (response.status === 401) {
          const redirect = encodeURIComponent(location.pathname + location.search);
          location.assign('/login?redirect=' + redirect);
          return;
        }
        message.textContent = localizeErrorMessage(result, response.status);
        if (form.hasAttribute('data-checkout') && response.status === 409) {
          invalidateCheckoutPreview();
          form.querySelector('[data-refresh-preview]').hidden = false;
        }
        return;
      }
      const target = form.dataset.success;
      if (result?.paymentUrl) { location.assign(result.paymentUrl); return; }
      if (result?.order?.paymentUrl) { location.assign(result.order.paymentUrl); return; }
      if(form.dataset.buyNow==='true'&&result?.items){const selected=result.items.find(i=>i.productId===body.productId&&(i.variantId||null)===(body.variantId||null));if(selected){location.assign(guestPrefix+'/checkout?selectedItemIds='+selected.itemId);return;}}
      if (target === 'reload') location.reload(); else location.assign(target.replace('{id}',String(result?.id || result?.order?.id)));
    } catch (error) {
      message.textContent = form.hasAttribute('data-checkout') ? 'Chưa nhận được phản hồi. Giữ trang này và gửi lại cùng yêu cầu để kiểm tra đơn đã tạo.' : 'Không nhận được phản hồi. Vui lòng kiểm tra kết nối.';
    } finally { form.dataset.busy = 'false'; button.disabled = form.hasAttribute('data-checkout')&&form.dataset.previewReady!=='true'; }
  });
}

const optionNames=['fulfillmentType','pickupBranchId','latitude','longitude','tradeInRequestId','installmentPlanId'];
function checkoutOptions(form) {
  const value=name=>form.elements[name]?.value;
  const number=name=>value(name)?Number(value(name)):null;
  const type=value('fulfillmentType')||'HOME_DELIVERY_STANDARD';
  return {fulfillmentType:type,pickupBranchId:type==='CLICK_AND_COLLECT'?number('pickupBranchId'):null,latitude:type==='HOME_DELIVERY_INSTANT_2H'?number('latitude'):null,longitude:type==='HOME_DELIVERY_INSTANT_2H'?number('longitude'):null,tradeInRequestId:number('tradeInRequestId'),installmentPlanId:value('paymentMethod')==='INSTALLMENT'?number('installmentPlanId'):null};
}
let previewSequence=0,previewAbort;
function invalidateCheckoutPreview() {
  previewSequence++;previewAbort?.abort();
  if(!checkout)return;
  checkout.dataset.previewReady='false';checkout.querySelector('[data-place-order]').disabled=true;
}
const selectedCheckoutIds=new URLSearchParams(location.search).get('selectedItemIds')?.split(',').map(Number)||null;
function checkoutPricingInput(form) {return {couponCode:form.elements.couponCode.value.trim()||null,options:checkoutOptions(form),selectedItemIds:selectedCheckoutIds};}
function syncCheckoutFields() {
  if(!checkout)return;
  const type=checkout.elements.fulfillmentType.value;
  const enable=(name,enabled)=>{const field=checkout.elements[name];if(field){field.disabled=!enabled;field.required=enabled;}};
  enable('pickupBranchId',type==='CLICK_AND_COLLECT');
  for(const name of ['latitude','longitude'])enable(name,type==='HOME_DELIVERY_INSTANT_2H');
  const coordinates=checkout.querySelector('[data-instant-coordinates]');if(coordinates)coordinates.open=type==='HOME_DELIVERY_INSTANT_2H';
  const isInstallment = checkout.elements.paymentMethod.value==='INSTALLMENT';
  enable('installmentPlanId', isInstallment);
  const instContainer = checkout.querySelector('[data-installment-container]');
  if(instContainer) instContainer.style.display = isInstallment ? 'block' : 'none';
}
async function updateInstallmentQuote() {
  if(!checkout || checkout.elements.paymentMethod.value !== 'INSTALLMENT') return;
  const planSelect = checkout.elements.installmentPlanId;
  const detailsBox = checkout.querySelector('[data-installment-details]');
  if(!planSelect || !detailsBox) return;
  const planId = planSelect.value;
  const total = Number(checkout.elements.expectedTotal?.value || 0);
  if(!planId || total <= 0) {
    detailsBox.style.display = 'none';
    return;
  }
  const selectedOption = planSelect.selectedOptions[0];
  const feePct = selectedOption?.dataset.fee || '0';
  try {
    const res = await fetch('/api/v1/installment/quote?planId=' + encodeURIComponent(planId) + '&amount=' + encodeURIComponent(total));
    if(!res.ok) throw new Error('Không tính được trả góp');
    const schedule = await res.json();
    detailsBox.querySelector('[data-inst-principal]').textContent = money(schedule.principal);
    detailsBox.querySelector('[data-inst-fee]').textContent = money(schedule.fee);
    detailsBox.querySelector('[data-inst-fee-pct]').textContent = feePct;
    detailsBox.querySelector('[data-inst-total]').textContent = money(schedule.total);
    const monthly = schedule.monthlyPayments && schedule.monthlyPayments.length > 0 ? schedule.monthlyPayments[0] : 0;
    detailsBox.querySelector('[data-inst-monthly]').textContent = money(monthly);
    const scheduleContainer = detailsBox.querySelector('[data-inst-schedule]');
    if(scheduleContainer && schedule.monthlyPayments) {
      scheduleContainer.innerHTML = schedule.monthlyPayments.map((p, idx) => `
        <div style="display:flex; justify-content:space-between; padding:3px 0; border-bottom:1px dashed var(--border);">
          <span>Kỳ ${idx + 1} (Tháng ${idx + 1})</span>
          <strong>${money(p)}</strong>
        </div>
      `).join('');
    }
    detailsBox.style.display = 'block';
  } catch(e) {
    detailsBox.style.display = 'none';
  }
}
async function updateCheckoutPreview() {
  const form=document.querySelector('[data-checkout]');if(!form)return;
  if(form.dataset.busy==='true')return;
  invalidateCheckoutPreview();syncCheckoutFields();
  const sequence=previewSequence,input=checkoutPricingInput(form);previewAbort=new AbortController();
  const signal=previewAbort.signal;
  const message=form.querySelector('.form-message');
  message.textContent='Đang tính giá...';
  try {
    const response=await fetch('/api/v1'+guestPrefix+'/orders/preview',{method:'POST',headers:{'Content-Type':'application/json','X-CSRF-TOKEN':token()},body:JSON.stringify(input),signal});
    const result=await response.json();if(!response.ok)throw Error(result.message);
    if(sequence!==previewSequence||JSON.stringify(input)!==JSON.stringify(checkoutPricingInput(form)))return;
    form.elements.cartVersion.value=result.cartVersion;form.elements.expectedTotal.value=result.total;form.elements.expectedPricingHash.value=result.pricingHash;
    document.querySelector('[data-preview-fee]').textContent=money(result.shippingFee);document.querySelector('[data-preview-total]').textContent=money(result.total);document.querySelector('[data-preview-discount]').textContent=money(result.discountAmount);
    document.querySelector('[data-preview-branch]').textContent=result.assignedBranchId?'Xuất máy tại chi nhánh #'+result.assignedBranchId:'';
    updateInstallmentQuote();
  } catch(error) {
    if(error.name==='AbortError'||sequence!==previewSequence)return;
    if(error.message && error.message.includes('Đặt sản phẩm theo IMEI và sản phẩm thường trong các đơn riêng')) {
      message.innerHTML = '⚠️ <strong>Không thể tính giá:</strong> Đơn hàng có cả thiết bị IMEI và phụ kiện. Vui lòng <a href="'+(guestPrefix?guestPrefix+'/cart':'/cart')+'" style="color:var(--primary);text-decoration:underline;">quay lại giỏ hàng</a> để chọn thanh toán từng nhóm hàng riêng.';
    } else {
      message.textContent=error.message||'Không cập nhật được giá.';
    }
    form.querySelector('[data-place-order]').disabled=true;
  }
}
const checkout=document.querySelector('[data-checkout]');
const savedAddress=document.querySelector('[data-saved-address]');
if(savedAddress&&checkout){
  const applyAddress=()=>{const option=savedAddress.selectedOptions[0];if(!option.dataset.name)return;for(const [name,key] of [['recipientName','name'],['phone','phone'],['address','address']])checkout.elements[name].value=option.dataset[key];};
  savedAddress.addEventListener('change',applyAddress);
  const preferred=[...savedAddress.options].find(o=>o.dataset.default==='true');if(preferred){savedAddress.value=preferred.value;applyAddress();}
}
const selectedCart=()=>[...document.querySelectorAll('[data-cart-select]:checked')].map(e=>Number(e.value));
const selectMessage=document.querySelector('[data-selection-message]');

// Quick select buttons for mixed carts
document.querySelectorAll('[data-filter-group]').forEach(btn => {
  btn.addEventListener('click', () => {
    const group = btn.dataset.filterGroup;
    document.querySelectorAll('[data-cart-select]').forEach(cb => {
      const isImei = !!cb.dataset.variantId;
      if (group === 'imei') cb.checked = isImei && cb.dataset.available !== 'false';
      else if (group === 'standard') cb.checked = !isImei && cb.dataset.available !== 'false';
      else cb.checked = cb.dataset.available !== 'false';
    });
    if (selectMessage) selectMessage.textContent = '';
  });
});

document.querySelector('[data-checkout-selected]')?.addEventListener('click',()=>{
  const selectedBoxes = [...document.querySelectorAll('[data-cart-select]:checked')];
  if(!selectedBoxes.length){selectMessage.textContent='Chọn ít nhất một sản phẩm.';return;}
  if(selectedBoxes.some(cb => cb.dataset.available === 'false')){selectMessage.textContent='Bỏ chọn sản phẩm chưa đủ điều kiện đặt hàng.';return;}

  const hasImei = selectedBoxes.some(cb => !!cb.dataset.variantId);
  const hasStandard = selectedBoxes.some(cb => !cb.dataset.variantId);
  if(hasImei && hasStandard) {
    selectMessage.textContent='Giỏ hàng đang chọn cả thiết bị IMEI và phụ kiện. Hãy dùng nút lọc bên trên để đặt từng nhóm đơn riêng.';
    return;
  }

  const ids = selectedBoxes.map(cb => Number(cb.value));
  location.assign(guestPrefix+'/checkout?selectedItemIds='+encodeURIComponent(ids.join(',')));
});
document.querySelector('[data-remove-selected]')?.addEventListener('click',async event=>{
  const ids=selectedCart();if(!ids.length){selectMessage.textContent='Chọn dòng cần xóa.';return;}event.target.disabled=true;
  try{const response=await fetch('/api/v1'+guestPrefix+'/cart/items/remove-many',{method:'POST',headers:{'Content-Type':'application/json','X-CSRF-TOKEN':token()},body:JSON.stringify({itemIds:ids,expectedVersion:Number(document.querySelector('[data-cart-version]').dataset.cartVersion)})});
    if(response.ok)location.reload();else{const result=await response.json();selectMessage.textContent=result.message;}
  }catch{selectMessage.textContent='Chưa xóa được. Hãy thử lại.';}finally{event.target.disabled=false;}
});
if(checkout) {
  checkout.addEventListener('input',event=>{
    if([...optionNames,'couponCode','paymentMethod'].includes(event.target.name)) {
      invalidateCheckoutPreview();syncCheckoutFields();
      if(event.target.name === 'installmentPlanId') updateInstallmentQuote();
      checkout.querySelector('.form-message').textContent='Lựa chọn đã đổi. Hãy tính lại giá trước khi đặt.';
    }
  });
  checkout.addEventListener('change',event=>{
    if(event.target.name === 'installmentPlanId') updateInstallmentQuote();
    if(event.target.name === 'paymentMethod') {
      syncCheckoutFields();
      updateInstallmentQuote();
    }
  });
  checkout.querySelector('[data-refresh-preview]').addEventListener('click',updateCheckoutPreview);
  updateCheckoutPreview();
}
const selector=document.querySelector('[data-variant]');
document.querySelector('[data-buy-now]')?.addEventListener('click',()=>{const form=document.querySelector('#product-cart');form.dataset.buyNow='true';form.requestSubmit();});
document.querySelector('#product-cart button[type=submit],#product-cart button:not([type])')?.addEventListener('click',()=>{document.querySelector('#product-cart').dataset.buyNow='false';});
const regionFilters=document.querySelector('[data-region-filters]');
let storeAbort,albumAbort;
async function updateStores() {
  if(!selector?.value)return;
  storeAbort?.abort();storeAbort=new AbortController();const signal=storeAbort.signal;
  const container=document.querySelector('[data-stores]');container.textContent='Đang kiểm tra cửa hàng...';
  const params=new URLSearchParams();
  for(const key of ['provinceCode','districtCode']) {const value=regionFilters?.querySelector('[name='+key+']')?.value;if(value)params.set(key,value);}
  if(regionFilters?.querySelector('[name=inStock]')?.checked)params.set('inStock','true');
  try {
    const response=await fetch('/api/v1/variants/'+selector.value+'/stores?'+params,{signal});if(!response.ok)throw Error();const stores=await response.json();
    if(signal.aborted)return;
    container.replaceChildren(...stores.map(s=>{const p=document.createElement('p');p.textContent=s.name+' · '+s.address+' · '+(s.available>0?'còn '+s.available+' máy':'Hết hàng');return p;}));
    if(!stores.length)container.textContent='Không có cửa hàng phù hợp. Thử chọn khu vực khác hoặc bỏ lọc còn hàng.';
  } catch(error) {if(error.name!=='AbortError')container.textContent='Chưa tải được tồn kho cửa hàng.';}
}
if(selector)selector.addEventListener('change',async()=>{
  const opt=selector.selectedOptions[0];document.querySelector('.large-price').textContent=money(opt.dataset.price);
  document.querySelector('[data-variant-stock]').textContent='Còn '+opt.dataset.stock+' máy trên hệ thống';
  const form=document.querySelector('#product-cart');if(form) {for(const button of form.querySelectorAll('button'))button.disabled=Number(opt.dataset.stock)<=0;form.elements.quantity.max=Math.min(99,Math.max(1,Number(opt.dataset.stock)));}
  updateStores();albumAbort?.abort();albumAbort=new AbortController();const signal=albumAbort.signal;
  const variantId=Number(selector.value);
  try {
    const response=await fetch('/api/v1/products/'+selector.dataset.productId+'/variants',{signal});if(!response.ok)return;
    const variants=await response.json();if(signal.aborted)return;const selected=variants.find(v=>v.id===variantId);
    const images=selected?.images?.length?selected.images:[document.querySelector('#main-product-img').dataset.defaultSrc];
    document.querySelector('#main-product-img').src=images[0];
    const album=document.querySelector('[data-product-album]');if(album)album.replaceChildren(...images.map(src=>{const button=document.createElement('button');button.type='button';button.className='album-thumb';const img=document.createElement('img');img.src=src;img.alt=opt.textContent;button.append(img);button.addEventListener('click',()=>{document.querySelector('#main-product-img').src=src;});return button;}));
  } catch(error) { /* Retain current album if the request fails. */ }
});
if(regionFilters) {
  const province=regionFilters.querySelector('[name=provinceCode]'),district=regionFilters.querySelector('[name=districtCode]');
  let branches=[];
  const options=(field,values,label)=>{field.replaceChildren(new Option(label,''),...values.map(v=>new Option(v,v)));};
  const districts=()=>{options(district,[...new Set(branches.filter(b=>!province.value||b.provinceCode===province.value).map(b=>b.districtCode).filter(Boolean))].sort(),'Tất cả khu vực');};
  const filterCards=()=>{let visible=0;for(const card of document.querySelectorAll('[data-branch-card]')) {card.hidden=!!((province.value&&card.dataset.province!==province.value)||(district.value&&card.dataset.district!==district.value));if(!card.hidden)visible++;}const empty=document.querySelector('[data-branches-empty]');if(empty)empty.hidden=visible>0;};
  regionFilters.addEventListener('change',event=>{if(event.target===province)districts();filterCards();updateStores();});
  fetch('/api/v1/branches').then(r=>{if(!r.ok)throw Error();return r.json();}).then(result=>{branches=result;options(province,[...new Set(branches.map(b=>b.provinceCode))].sort(),'Tất cả tỉnh / thành');districts();}).catch(()=>{regionFilters.querySelector('[data-region-message]').textContent='Chưa tải được khu vực. Danh sách cửa hàng vẫn hiển thị bên dưới.';});
}

const searchField=document.querySelector('#header-keyword');
if(searchField) {
  let searchTimer,searchAbort;
  const suggestionBox=document.querySelector('[data-search-suggestions]');
  searchField.addEventListener('input',()=>{
    clearTimeout(searchTimer);searchAbort?.abort();suggestionBox.replaceChildren();
    if(searchField.value.trim().length<2)return;
    searchTimer=setTimeout(async()=>{
      searchAbort=new AbortController();
      try {
        const response=await fetch('/api/v1/products/autocomplete?keyword='+encodeURIComponent(searchField.value.trim()),{signal:searchAbort.signal});
        if(!response.ok)return;const result=await response.json();
        suggestionBox.replaceChildren(...result.content.map(p=>{const a=document.createElement('a');a.href='/products/'+p.id;a.textContent=p.name+' · '+money(p.price);return a;}));
      } catch(error) {if(error.name!=='AbortError')suggestionBox.replaceChildren();}
    },250);
  });
  document.addEventListener('click',event=>{if(!event.target.closest('.header-search'))suggestionBox.replaceChildren();});
}

const scanButton=document.querySelector('[data-scan-camera]');
if(scanButton) {
  const video=document.querySelector('[data-scan-video]'),stopButton=document.querySelector('[data-stop-camera]'),message=document.querySelector('[data-scan-message]');
  let camera,scanning=false;
  function stopCamera(){scanning=false;camera?.getTracks().forEach(t=>t.stop());video.srcObject=null;video.hidden=true;stopButton.hidden=true;scanButton.disabled=false;}
  stopButton.addEventListener('click',stopCamera);window.addEventListener('pagehide',stopCamera);
  scanButton.addEventListener('click',async()=>{
    if(!('BarcodeDetector' in window)||!navigator.mediaDevices){message.textContent='Trình duyệt chưa hỗ trợ quét camera. Dùng máy quét USB hoặc nhập mã thủ công.';return;}
    try {
      camera=await navigator.mediaDevices.getUserMedia({video:{facingMode:'environment'}});video.srcObject=camera;video.hidden=false;stopButton.hidden=false;scanButton.disabled=true;await video.play();scanning=true;
      const supported=await BarcodeDetector.getSupportedFormats();const detector=new BarcodeDetector({formats:supported.filter(s=>['code_128','code_39','ean_13','qr_code'].includes(s))});
      message.textContent='Đưa mã vào khung camera.';
      while(scanning){const codes=await detector.detect(video);if(codes.length){const serial=codes[0].rawValue.trim();if(/^[A-Za-z0-9-]{5,40}$/.test(serial)){const isPickup=scanButton.dataset.scanTarget==='pickup';const field=document.querySelector(isPickup?'form[data-api$="/pickup"] [name=pickupCode]':'form[data-api$="/imei/import"] [name=imeiNumbers]');field.value=isPickup?serial:field.value+(field.value?'\n':'')+serial;message.textContent=isPickup?'Đã đọc mã. Kiểm tra đơn trước khi xác nhận.':'Đã thêm serial: '+serial;stopCamera();break;}}await new Promise(resolve=>setTimeout(resolve,200));}
    } catch(error){stopCamera();message.textContent='Chưa mở được camera. Kiểm tra quyền hoặc dùng máy quét USB.';}
  });
}

// Sticky header & Back-to-top handler
window.addEventListener('scroll', () => {
  const header = document.querySelector('.site-header');
  const btt = document.getElementById('btn-back-to-top');
  if (header) {
    if (window.scrollY > 40) header.classList.add('sticky');
    else header.classList.remove('sticky');
  }
  if (btt) {
    if (window.scrollY > 250) btt.classList.add('show');
    else btt.classList.remove('show');
  }
});

// Storefront carousel: keyboard controls, explicit pause and reduced-motion support.
const storefrontSlider = document.querySelector('[data-storefront-slider]');
if (storefrontSlider) {
  const slides = [...storefrontSlider.querySelectorAll('.hero-slide')];
  const tabs = [...storefrontSlider.querySelectorAll('.slider-dot')];
  const pauseButton = storefrontSlider.querySelector('[data-slide-pause]');
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  let current = 0, timer, paused = reducedMotion.matches, hovering = false;
  const canRotate = () => !paused && !hovering && !storefrontSlider.contains(document.activeElement) && !document.hidden;
  const showSlide = index => {
    current = (index + slides.length) % slides.length;
    slides.forEach((slide, i) => {
      slide.classList.toggle('active', i === current);
      slide.setAttribute('aria-hidden', String(i !== current));
      slide.inert = i !== current;
    });
    tabs.forEach((tab, i) => {
      tab.classList.toggle('active', i === current);
      tab.setAttribute('aria-pressed', String(i === current));
    });
  };
  const syncTimer = () => {
    clearInterval(timer);
    if (canRotate()) timer = setInterval(() => showSlide(current + 1), 6000);
  };
  const syncPause = () => {
    pauseButton.setAttribute('aria-pressed', String(paused));
    pauseButton.setAttribute('aria-label', paused ? 'Tiếp tục chuyển banner' : 'Tạm dừng chuyển banner');
    pauseButton.textContent = paused ? '▶' : 'Ⅱ';
    syncTimer();
  };
  tabs.forEach((tab, i) => tab.addEventListener('click', () => { showSlide(i); syncTimer(); }));
  storefrontSlider.querySelectorAll('[data-slide-step]').forEach(button => button.addEventListener('click', () => { showSlide(current + Number(button.dataset.slideStep)); syncTimer(); }));
  pauseButton.addEventListener('click', () => { paused = !paused; syncPause(); });
  storefrontSlider.addEventListener('pointerenter', () => { hovering = true; syncTimer(); });
  storefrontSlider.addEventListener('pointerleave', () => { hovering = false; syncTimer(); });
  storefrontSlider.addEventListener('focusin', syncTimer);
  storefrontSlider.addEventListener('focusout', () => setTimeout(syncTimer, 0));
  document.addEventListener('visibilitychange', syncTimer);
  reducedMotion.addEventListener('change', event => { if (event.matches) { paused = true; syncPause(); } });
  window.addEventListener('pagehide', () => clearInterval(timer));
  window.addEventListener('pageshow', syncTimer);
  showSlide(0); syncPause();
}

const filterToggle = document.querySelector('[data-filter-toggle]');
if (filterToggle) {
  const filterPanel = document.getElementById(filterToggle.getAttribute('aria-controls'));
  const setFiltersOpen = open => { filterPanel.hidden = !open; filterToggle.setAttribute('aria-expanded', String(open)); };
  const activeFilters = [...filterPanel.querySelectorAll('input, select')].some(field => field.name === 'sort' ? field.value !== 'newest' : field.value.trim() !== '');
  setFiltersOpen(!window.matchMedia('(max-width: 560px)').matches || activeFilters);
  filterToggle.addEventListener('click', () => setFiltersOpen(filterPanel.hidden));
}

// Session-local comparison selection survives category filters and pagination.
const compareTray = document.querySelector('[data-compare-tray]');
if (compareTray) {
  const storageKey = 'techshop.compare.v1';
  const compareButtons = [...document.querySelectorAll('[data-compare-id]')];
  const summary = compareTray.querySelector('[data-compare-summary]');
  const link = compareTray.querySelector('[data-compare-link]');
  const message = document.querySelector('[data-compare-message]');
  let selection = [];
  try {
    const stored = JSON.parse(sessionStorage.getItem(storageKey) || '[]');
    if (Array.isArray(stored)) selection = stored.filter(item => item && /^\d+$/.test(item.id) && typeof item.name === 'string').filter((item, i, list) => list.findIndex(other => other.id === item.id) === i).slice(0, 3);
  } catch (error) { /* A restricted or invalid session store still allows in-memory selection. */ }
  const renderComparison = () => {
    try { sessionStorage.setItem(storageKey, JSON.stringify(selection)); } catch (error) { /* Storage may be disabled. */ }
    compareTray.hidden = selection.length === 0;
    summary.textContent = selection.length === 1 ? 'Chọn thêm 1 sản phẩm để so sánh · ' + selection[0].name : selection.map(item => item.name).join(' · ');
    const ready = selection.length >= 2;
    link.href = ready ? '/compare?ids=' + selection.map(item => item.id).join(',') : '/compare';
    link.setAttribute('aria-disabled', String(!ready));
    link.tabIndex = ready ? 0 : -1;
    compareButtons.forEach(button => {
      const selected = selection.some(item => item.id === button.dataset.compareId);
      button.setAttribute('aria-pressed', String(selected));
      button.setAttribute('aria-label', (selected ? 'Bỏ ' : 'Thêm ') + button.dataset.compareName + (selected ? ' khỏi so sánh' : ' vào so sánh'));
    });
  };
  compareButtons.forEach(button => button.addEventListener('click', () => {
    const id = button.dataset.compareId, index = selection.findIndex(item => item.id === id);
    if (index >= 0) selection.splice(index, 1);
    else if (selection.length < 3) selection.push({ id, name: button.dataset.compareName });
    else {
      const limitText = 'Chỉ so sánh tối đa 3 sản phẩm. Bỏ chọn một sản phẩm để thêm thiết bị khác.';
      summary.textContent = limitText; message.textContent = limitText; return;
    }
    renderComparison();
    message.textContent = selection.length + ' sản phẩm đã chọn để so sánh.';
  }));
  link.addEventListener('click', event => { if (selection.length < 2) event.preventDefault(); });
  compareTray.querySelector('[data-compare-clear]').addEventListener('click', () => {
    selection = []; renderComparison(); message.textContent = 'Đã xóa các sản phẩm so sánh.';
    compareButtons[0]?.focus();
  });
  renderComparison();
}

const headerMenus = [...document.querySelectorAll('.site-header details')];
headerMenus.forEach(menu => menu.addEventListener('toggle', () => {
  if (menu.open) headerMenus.filter(other => other !== menu).forEach(other => { other.open = false; });
}));
document.addEventListener('click', event => headerMenus.forEach(menu => { if (!menu.contains(event.target)) menu.open = false; }));
document.addEventListener('keydown', event => {
  if (event.key === 'Escape') headerMenus.forEach(menu => { if (menu.open) { menu.open = false; menu.querySelector('summary').focus(); } });
});
document.querySelectorAll('[data-print]').forEach(button => button.addEventListener('click', () => window.print()));
const resetToken=document.querySelector('[data-reset-token]');
if(resetToken){resetToken.value=new URLSearchParams(location.hash.slice(1)).get('token')||'';history.replaceState(null,'',location.pathname);}
document.querySelector('[data-recovery-request]')?.addEventListener('submit',async event=>{
  event.preventDefault();const form=event.target,button=form.querySelector('button'),message=form.querySelector('.form-message');if(button.disabled)return;button.disabled=true;
  try{const response=await fetch('/api/v1/auth/forgot-password',{method:'POST',headers:{'Content-Type':'application/json','X-CSRF-TOKEN':token()},body:JSON.stringify({email:form.elements.email.value})});const result=await response.json();message.textContent=result.message||'Chưa xử lý được yêu cầu.';}
  catch{message.textContent='Chưa nhận được phản hồi. Vui lòng thử lại.';}finally{button.disabled=false;}
});
document.querySelector('[data-avatar-upload]')?.addEventListener('submit',async event=>{
  event.preventDefault();const form=event.target,button=form.querySelector('button'),message=form.querySelector('.form-message');if(button.disabled)return;button.disabled=true;
  try{const response=await fetch('/api/v1/me/avatar',{method:'POST',headers:{'X-CSRF-TOKEN':token()},body:new FormData(form)});if(response.ok)location.reload();else{const result=await response.json();message.textContent=result.message;}}
  catch{message.textContent='Chưa tải ảnh được. Vui lòng thử lại.';}finally{button.disabled=false;}
});
document.querySelector('[data-newsletter-request]')?.addEventListener('submit',async event=>{
  event.preventDefault();const form=event.target,button=form.querySelector('button'),message=form.querySelector('.form-message');if(button.disabled)return;button.disabled=true;
  try{const response=await fetch('/api/v1/newsletter',{method:'POST',headers:{'Content-Type':'application/json','X-CSRF-TOKEN':token()},body:JSON.stringify({email:form.elements.email.value,consent:form.elements.consent.checked})});const result=await response.json();message.textContent=result.message;}
  catch{message.textContent='Chưa nhận được phản hồi. Hãy thử lại.';}finally{button.disabled=false;}
});
