'use strict';

const token = () => document.querySelector('meta[name="csrf-token"]').content;
const money = value => new Intl.NumberFormat('vi-VN').format(value) + ' ₫';
for (const form of document.querySelectorAll('form[data-api]')) {
  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (form.dataset.busy === 'true') return;
    const message = form.querySelector('.form-message');
    const button = form.querySelector('button[type="submit"],button:not([type])');
    const body = {};
    for (const field of form.elements) {
      if (!field.name || field.disabled) continue;
      body[field.name] = field.type === 'checkbox' ? field.checked : field.hasAttribute('data-number') ? Number(field.value) : field.value;
    }
    if ('note' in body && !body.note.trim()) body.note = null;
    form.dataset.busy = 'true'; button.disabled = true; message.textContent = '';
    try {
      const method = form.dataset.method || 'POST';
      const response = await fetch(form.dataset.api, {method, credentials:'same-origin', headers:{'Content-Type':'application/json','Accept':'application/json','X-CSRF-TOKEN':token()}, body:method === 'DELETE' ? undefined : JSON.stringify(body)});
      const result = response.status === 204 ? null : await response.json();
      if (!response.ok) {
        if (response.status === 401) { location.assign('/login'); return; }
        const fields = (result.fieldErrors || []).map(e => e.field + ': ' + e.message).join(' · ');
        message.textContent = (result.message || 'Chưa thể xử lý yêu cầu.') + (fields ? ' ' + fields : '');
        if (response.status === 409 && form.hasAttribute('data-checkout') && ['PRICE_CHANGED','CART_CHANGED','INSUFFICIENT_STOCK','PRODUCT_UNAVAILABLE'].includes(result.code)) {
          form.querySelector('[data-refresh-preview]').hidden = false;
        }
        if (response.status === 403) message.textContent += ' Vui lòng tải lại trang để kiểm tra phiên đăng nhập.';
        return;
      }
      const target = form.dataset.success;
      if (target === 'reload') location.reload(); else location.assign(target.replace('{id}',String(result?.id)));
    } catch (error) {
      message.textContent = form.hasAttribute('data-checkout') ? 'Chưa nhận được phản hồi. Giữ trang này và gửi lại cùng yêu cầu để kiểm tra đơn đã tạo.' : 'Không nhận được phản hồi. Vui lòng kiểm tra kết nối.';
    } finally { form.dataset.busy = 'false'; button.disabled = false; }
  });
}

const refresh = document.querySelector('[data-refresh-preview]');
if (refresh) refresh.addEventListener('click', async () => {
  const form = document.querySelector('[data-checkout]');
  const message = form.querySelector('.form-message');
  refresh.disabled = true;
  try {
    const response = await fetch('/api/v1/cart',{credentials:'same-origin',headers:{Accept:'application/json'}});
    if (!response.ok) throw new Error('Cannot refresh');
    const cart = await response.json();
    if (!cart.canCheckout) { message.textContent = 'Giỏ chưa đủ hàng hoặc đã thay đổi. Vui lòng quay lại giỏ để kiểm tra.'; return; }
    form.elements.cartVersion.value = cart.version;
    form.elements.expectedTotal.value = cart.total;
    form.elements.expectedPricingHash.value = cart.pricingHash;
    form.elements.checkoutKey.value = crypto.randomUUID();
    document.querySelector('[data-preview-fee]').textContent = money(cart.shippingFee);
    document.querySelector('[data-preview-total]').textContent = money(cart.total);
    const container = document.querySelector('[data-preview-lines]');
    if (container) container.replaceChildren(...cart.items.map(item => {
      const row = document.createElement('p'), label = document.createElement('span'), amount = document.createElement('strong');
      label.textContent = item.name + ' × ' + item.quantity; amount.textContent = money(item.lineTotal); row.append(label,amount); return row;
    }));
    message.textContent = 'Đã cập nhật giá. Kiểm tra lại đơn và bấm xác nhận khi bạn đồng ý.';
    refresh.hidden = true;
  } catch (error) { message.textContent = 'Chưa cập nhật được giá. Vui lòng thử lại.'; }
  finally { refresh.disabled = false; }
});
