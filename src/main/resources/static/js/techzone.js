"use strict";
// Quick add always uses the server's current cart version and real variant IDs.
const buyDialog=document.querySelector('[data-buy-dialog]');
const buyToast=document.querySelector('[data-cart-toast]');
let buyProduct=null,buyVariants=[],buyBusy=false,buyLoading=false,buyTrigger=null,toastTimer;
const csrfToken=()=>document.querySelector('meta[name="csrf-token"]')?.content;
const notifyCart=(message)=>{if(!buyToast)return;buyToast.textContent=message;buyToast.hidden=false;clearTimeout(toastTimer);toastTimer=setTimeout(()=>{buyToast.hidden=true},5000)};
async function cartJson(url,options={}){
 const response=await fetch(url,{credentials:'same-origin',...options});
 const result=await response.json();
 if(response.status===401){
  const redirect=encodeURIComponent(location.pathname+location.search);
  location.assign('/login?redirect='+redirect);
  throw new Error('Vui lòng đăng nhập để tiếp tục.');
 }
 if(!response.ok)throw new Error(result.message||'Chưa thể thêm sản phẩm. Vui lòng thử lại.');
 return result;
}
async function addQuickProduct(variantId){
 if(buyBusy)return;buyBusy=true;
 const confirm=buyDialog.querySelector('[data-buy-confirm]');const error=buyDialog.querySelector('[data-buy-error]');error.textContent='';confirm.disabled=true;
 try{
  const isCust=buyTrigger&&buyTrigger.dataset.customer==='true';
  const cartUrl=isCust?'/api/v1/cart':'/api/v1/guest/cart';
  const itemsUrl=isCust?'/api/v1/cart/items':'/api/v1/guest/cart/items';
  const cart=await cartJson(cartUrl);
  const next=await cartJson(itemsUrl,{method:'POST',headers:{'Content-Type':'application/json','X-CSRF-TOKEN':csrfToken()},body:JSON.stringify({productId:buyProduct.id,variantId,quantity:1,expectedVersion:cart.version})});
  document.querySelectorAll('[data-header-cart-count]').forEach(node=>{node.textContent=next.items.reduce((n,item)=>n+item.quantity,0)});
  buyDialog.close();notifyCart('Đã thêm '+buyProduct.name+' vào giỏ hàng.');
 }catch(e){error.textContent=e.message;if(!buyDialog.open)buyDialog.showModal();}
 finally{buyBusy=false;updateBuySelection();buyTrigger.disabled=false;}
}
function updateBuySelection(){
 const variant=buyVariants.find(v=>v.id===Number(buyDialog.querySelector('[data-buy-variant]').value));
 buyDialog.querySelector('[data-buy-stock]').textContent=variant?'Còn '+variant.available+' sản phẩm theo phiên bản đã chọn.':'Sản phẩm không có lựa chọn phiên bản.';
 buyDialog.querySelector('[data-buy-confirm]').disabled=!!variant&&variant.available<=0;
}
if(buyDialog){
 buyDialog.querySelector('[data-buy-variant]').addEventListener('change',updateBuySelection);
 buyDialog.querySelector('[data-buy-confirm]').addEventListener('click',()=>{const choice=buyDialog.querySelector('[data-buy-variant]').value;addQuickProduct(choice?Number(choice):null)});
 buyDialog.addEventListener('close',()=>{if(buyTrigger&&!buyBusy){buyTrigger.disabled=false;buyTrigger.focus()}});
 document.querySelectorAll('[data-quick-add]').forEach(button=>button.addEventListener('click',async()=>{
  if(buyBusy||buyLoading||buyDialog.open)return;
  buyLoading=true;
  buyTrigger=button;button.disabled=true;buyProduct={id:Number(button.dataset.quickAdd),name:button.dataset.productName};
  buyDialog.querySelector('[data-buy-name]').textContent=buyProduct.name;
  buyDialog.querySelector('[data-buy-detail]').href=(button.dataset.customer==='true'?'/products/':'/guest/products/')+buyProduct.id;
  buyDialog.querySelector('[data-buy-error]').textContent='';
  try{
   buyVariants=await cartJson('/api/v1/products/'+buyProduct.id+'/variants');
   const selector=buyDialog.querySelector('[data-buy-variant]');selector.replaceChildren();
   for(const v of buyVariants){const option=document.createElement('option');option.value=v.id;option.textContent=v.colorName+' / '+v.storageCapacity+' · '+new Intl.NumberFormat('vi-VN').format(v.price)+' ₫';option.disabled=v.available<=0;selector.append(option);}
   const available=buyVariants.find(v=>v.available>0);if(available)selector.value=available.id;
   selector.parentElement.hidden=!buyVariants.length;updateBuySelection();
   if(buyVariants.length===0)await addQuickProduct(null);
   else{buyDialog.showModal();button.disabled=false;buyDialog.querySelector('[data-buy-variant]').focus();}
  }catch(e){button.disabled=false;notifyCart(e.message);}
  finally{buyLoading=false;button.disabled=false;}
 }));
}
