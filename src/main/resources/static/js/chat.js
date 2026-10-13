'use strict';
(() => {
  const root=document.querySelector('[data-chat]');
  const launcher=document.querySelector('[data-chat-launcher]');
  if(root && launcher)launcher.hidden=true;
  const admin= root ? root.dataset.admin==='true' : launcher?.dataset.admin==='true';
  const badge=launcher?.querySelector('[data-chat-unread]');
  const apiRoot=admin?'/api/v1/admin/chat':'/api/v1/chat';
  async function request(url,method='GET',body) {
    const response=await fetch(url,{method,credentials:'same-origin',cache:'no-store',signal:AbortSignal.timeout(15000),headers:{Accept:'application/json',...(method!=='GET'?{'X-CSRF-TOKEN':document.querySelector('meta[name="csrf-token"]').content}:{}),...(body?{'Content-Type':'application/json'}:{})},...(body?{body:JSON.stringify(body)}:{})}).catch(()=>{throw new Error("Không nhận được phản hồi. Vui lòng kiểm tra kết nối và thử lại.");});
    if(response.status===401){if(root)location.assign('/login?redirect='+encodeURIComponent(location.pathname));throw new Error('Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.');}
    const result=response.status===204?null:await response.json();
    if(!response.ok)throw new Error(response.status===403?'Không có quyền thao tác hoặc phiên đã hết hạn. Vui lòng tải lại trang.':result?.message||'Không thể tải tin nhắn.');
    return result;
  }
  async function unread() {
    if(!badge || document.hidden)return;
    try{const value=await request(apiRoot+'/unread');badge.textContent=value.unreadCount>99?'99+':String(value.unreadCount);badge.hidden=value.unreadCount===0;}catch{}
  }
  if(!root){if(badge){unread();setInterval(unread,15000);}return;}
  const $=s=>root.querySelector(s),timeline=$('[data-chat-timeline]'),messageList=$('[data-chat-messages]'),empty=$('[data-chat-empty]'),older=$('[data-chat-older]'),form=$('[data-chat-form]'),input=form.elements.content,sendButton=form.querySelector('button[type="submit"]'),error=$('[data-chat-error]');
  let selected=null,generation=0,lastId=0,firstId=0,customerRead=0,adminRead=0,page=0,totalPages=0,loading=null,inboxLoading=false,sending=false;
  const rendered=new Set(),drafts=new Map(),pending=new Map();
  let support=null,supportBusy=false;
  const dates=new Intl.DateTimeFormat('vi-VN',{timeZone:'Asia/Ho_Chi_Minh',day:'2-digit',month:'2-digit',hour:'2-digit',minute:'2-digit'});
  const endpoint=()=>admin?apiRoot+'/conversations/'+selected+'/messages':apiRoot+'/messages';
  const readEndpoint=()=>admin?apiRoot+'/conversations/'+selected+'/read':apiRoot+'/read';
  const draftKey=()=>admin?selected:'customer';
  function count(){ $('[data-chat-length]').textContent=input.value.length+' / 2.000'; }
  function node(tag,cls,text){const element=document.createElement(tag);if(cls)element.className=cls;if(text!=null)element.textContent=text;return element;}
  function receipts(){for(const element of messageList.querySelectorAll('[data-message-own]')){const read=admin?customerRead:adminRead;element.querySelector('[data-receipt]').textContent=Number(element.dataset.id)<=read?'Đã xem':'Đã gửi';}}
  function add(rows,prepend=false){
    const fragment=document.createDocumentFragment();
    for(const m of rows){if(rendered.has(m.id))continue;rendered.add(m.id);
      const own=m.senderRole===(admin?'ADMIN':'CUSTOMER');
      const item=node('article','chat-message'+(own?' chat-message-own':'')+(m.senderRole==='AI'?' chat-message-ai':''));item.dataset.id=m.id;
      if(own)item.dataset.messageOwn='true';
      item.append(node('span','chat-message-author',own?'Bạn':m.senderRole==='AI'?'Trợ lý AI · Gemini':m.senderRole==='ADMIN'?'TechZone · '+m.senderName:m.senderName));
      item.append(node('div','chat-bubble',m.content));
      if(m.senderRole==='AI' && Array.isArray(m.products) && m.products.length){
        const cards=node('div','chat-product-cards');
        for(const p of m.products){
          if(!Number.isSafeInteger(p.id)||p.id<=0)continue;
          const card=node('a','chat-product-card');card.href='/products/'+p.id;
          const img=node('img');img.src=typeof p.image==='string'&&p.image.startsWith('/assets/')&&!p.image.includes('..')?p.image:'/assets/products/device.svg';img.alt='';img.loading='lazy';card.append(img);
          const copy=node('span');copy.append(node('strong',null,p.name),node('span','chat-product-price','Giá từ '+new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND'}).format(p.price)),node('small',null,'Còn '+p.available+' · Xem phiên bản và giá hiện tại'));card.append(copy);cards.append(card);
        }
        item.append(cards,node('small','chat-ai-note','Giá và tồn kho tại lúc tư vấn; hãy kiểm tra lại trên trang sản phẩm.'));
      }
      const meta=node('div','chat-message-meta');const time=node('time',null,dates.format(new Date(m.createdAt)));time.dateTime=m.createdAt;meta.append(time);
      if(own){meta.append(document.createTextNode(' · '));const receipt=node('span');receipt.dataset.receipt='true';meta.append(receipt);}
      item.append(meta);fragment.append(item);
    }
    if(prepend)messageList.prepend(fragment);else messageList.append(fragment);
    empty.hidden=rendered.size>0;receipts();
  }
  function clear(){rendered.clear();messageList.replaceChildren();lastId=0;firstId=0;customerRead=0;adminRead=0;older.hidden=true;empty.hidden=false;}
  async function markRead(revision){
    if(!lastId || document.hidden || revision!==generation)return;
    await request(readEndpoint(),'POST',{upToId:lastId});
    if(revision===generation){if(admin)adminRead=Math.max(adminRead,lastId);else customerRead=Math.max(customerRead,lastId);unread();}
  }
  async function refreshMessages(reset=false){
    if(admin && !selected)return;
    if(loading?.generation===generation)return loading.promise;
    const revision=generation,url=endpoint();
    const promise=(async()=>{
      try{
        const nearBottom=timeline.scrollHeight-timeline.scrollTop-timeline.clientHeight<100;
        let more=true;
        while(more){
          const result=await request(url+(lastId?'?afterId='+lastId:''));
          if(revision!==generation)return;
          customerRead=result.customerReadId;adminRead=result.adminReadId;updateSupport(result.support);
          const initial=!lastId;
          if(initial)older.hidden=!result.hasMore;
          add(result.messages);
          if(result.messages.length){firstId=firstId||result.messages[0].id;lastId=result.messages.at(-1).id;}
          more=!initial && result.hasMore && result.messages.length>0;
        }
        if(reset||nearBottom)timeline.scrollTop=timeline.scrollHeight;
        await markRead(revision);
        if(revision===generation){error.textContent='';$('[data-chat-status]').textContent=admin?'Đang xem cuộc trò chuyện riêng với khách hàng.':'Tin nhắn được lưu · Tự cập nhật mỗi 3 giây';}
      }catch(e){if(revision===generation)error.textContent=e.message||'Không nhận được phản hồi. Kiểm tra kết nối rồi thử lại.';}
    })();
    loading={generation:revision,promise};try{await promise;}finally{if(loading?.promise===promise)loading=null;}
  }
  function select(c){
    if(selected===c.id)return;
    drafts.set(draftKey(),input.value);selected=c.id;generation++;clear();
    input.value=drafts.get(draftKey())||'';input.disabled=false;sendButton.disabled=sending;count();error.textContent='';
    $('[data-chat-title]').textContent=c.customerName;$('[data-thread-avatar]').textContent=c.customerName.trim().slice(0,2).toUpperCase();
    $('[data-chat-status]').textContent='Đang tải tin nhắn…';
    for(const b of root.querySelectorAll('[data-conversation-id]'))b.setAttribute('aria-pressed',String(Number(b.dataset.conversationId)===selected));
    refreshMessages(true);
  }
  async function inbox(){
    if(!admin||inboxLoading)return;inboxLoading=true;
    try{
      const result=await request(apiRoot+'/conversations?page='+page);totalPages=result.totalPages;
      if(totalPages && page>=totalPages){page=totalPages-1;return;}
      const container=$('[data-chat-conversations]'),focusedId=document.activeElement?.dataset.conversationId;
      container.replaceChildren();$('[data-inbox-total]').textContent=result.totalElements;
      for(const c of result.items){
        const button=node('button','chat-conversation');button.type='button';button.dataset.conversationId=c.id;button.setAttribute('aria-pressed',String(selected===c.id));
        button.append(node('span','chat-avatar',c.customerName.trim().slice(0,2).toUpperCase()));
        const copy=node('span','chat-conversation-copy');copy.append(node('strong',null,c.customerName),node('small','chat-conversation-mode',c.supportMode==='AI'?'AI đang hỗ trợ':c.handoffStatus==='WAITING'?'Chờ nhân viên':c.handoffStatus==='ACCEPTED'?'Nhân viên đang hỗ trợ':c.handoffStatus==='CLOSED'?'Đã kết thúc':'Hỗ trợ khách hàng'),node('small',null,c.lastMessage));button.append(copy);
        if(c.unreadCount)button.append(node('span','chat-unread',c.unreadCount>99?'99+':String(c.unreadCount)));
        button.addEventListener('click',()=>select(c));container.append(button);
        if(focusedId===String(c.id))button.focus({preventScroll:true});
      }
      if(!result.items.length)container.append(node('p','chat-placeholder','Chưa có cuộc trò chuyện. Tin nhắn mới của khách hàng sẽ xuất hiện tại đây.'));
      $('[data-inbox-page]').textContent='Trang '+(page+1)+' / '+Math.max(totalPages,1);$('[data-inbox-prev]').disabled=page===0;$('[data-inbox-next]').disabled=page+1>=totalPages;
    }catch(e){error.textContent=e.message||'Không tải được hộp thư.';}finally{inboxLoading=false;}
  }
  function updateSupport(value){
    if(!value)return;support=value;
    $('[data-support-status]').textContent=value.supportMode==='AI'?(value.aiStatus==='PROCESSING'?'AI đang trả lời…':value.aiStatus==='QUEUED'?'AI đang chờ xử lý câu hỏi…':'Trợ lý AI đang hỗ trợ. Bạn có thể chuyển sang nhân viên bất cứ lúc nào.'):value.handoffStatus==='ACCEPTED'?'Nhân viên đang hỗ trợ cuộc trò chuyện.':value.handoffStatus==='WAITING'?(value.aiStatus==='FAILED'?'AI chưa trả lời được. Đã chuyển yêu cầu sang nhân viên.':'Yêu cầu đã được chuyển sang nhân viên.'):value.handoffStatus==='CLOSED'?'Nhân viên đã kết thúc hỗ trợ. Bạn có thể bật AI hoặc gửi yêu cầu mới.':'Chat với nhân viên hỗ trợ TechZone.';
    if(admin){$('[data-admin-accept]').disabled=supportBusy||!selected||value.handoffStatus==='ACCEPTED';$('[data-admin-close]').disabled=supportBusy||!selected||value.handoffStatus!=='ACCEPTED';}
    else{
      const open=$('[data-ai-open]');open.disabled=supportBusy||!value.available||value.supportMode==='AI'||value.handoffStatus==='ACCEPTED';open.textContent=!value.available?'AI chưa được cấu hình':value.supportMode==='AI'?'AI đang bật':'Tư vấn bằng AI';
      $('[data-human-handoff]').disabled=supportBusy||['WAITING','ACCEPTED'].includes(value.handoffStatus);
    }
  }
  async function supportAction(action,payload){
    if(supportBusy||admin&&!selected)return;supportBusy=true;updateSupport(support);const revision=generation;
    try{const result=await request(admin?apiRoot+'/conversations/'+selected+'/'+action:apiRoot+'/'+action,'POST',payload);if(revision===generation){updateSupport(result);error.textContent='';if(!admin)$('[data-ai-consent]').hidden=true;}await inbox();}
    catch(e){if(revision===generation)error.textContent=e.message;}
    finally{supportBusy=false;if(revision===generation)updateSupport(support);}
  }
  if(admin){
    $('[data-admin-accept]').addEventListener('click',()=>supportAction('accept'));
    $('[data-admin-close]').addEventListener('click',()=>supportAction('close-support'));
    request(apiRoot+'/ai/status').then(value=>{$('[data-ai-configuration]').textContent=value.available?'Gemini: '+value.model+' · '+value.requestsToday+' / '+value.dailyRequestLimit+' yêu cầu hôm nay':'Gemini chưa bật hoặc chưa đủ cấu hình API key và model.';}).catch(()=>{});
  }else{
    $('[data-ai-open]').addEventListener('click',()=>{$('[data-ai-consent]').hidden=false;$('[data-ai-agree]').checked=false;$('[data-ai-start]').disabled=true;});
    $('[data-ai-agree]').addEventListener('change',()=>{$('[data-ai-start]').disabled=!$('[data-ai-agree]').checked;});
    $('[data-ai-cancel]').addEventListener('click',()=>{$('[data-ai-consent]').hidden=true;});
    $('[data-ai-start]').addEventListener('click',()=>{if($('[data-ai-agree]').checked)supportAction('ai/start',{consent:true});});
    $('[data-human-handoff]').addEventListener('click',()=>supportAction('handoff'));
  }
  older.addEventListener('click',async()=>{
    if(!firstId)return;const revision=generation,height=timeline.scrollHeight;older.disabled=true;
    try{const result=await request(endpoint()+'?beforeId='+firstId);if(revision!==generation)return;
      add(result.messages,true);if(result.messages.length)firstId=result.messages[0].id;older.hidden=!result.hasMore;timeline.scrollTop+=timeline.scrollHeight-height;
    }catch(e){if(revision===generation)error.textContent=e.message;}finally{older.disabled=false;}
  });
  input.addEventListener('input',count);
  input.addEventListener('keydown',event=>{if(event.key==='Enter'&&!event.shiftKey&&!event.isComposing){event.preventDefault();form.requestSubmit();}});
  form.addEventListener('submit',async event=>{
    event.preventDefault();if(sending || admin&&!selected)return;
    const content=input.value.trim();if(!content){error.textContent='Vui lòng nhập nội dung tin nhắn.';return;}
    const revision=generation,key=draftKey(),url=endpoint();
    let payload=pending.get(key);if(!payload||payload.content!==content){payload={content,clientMessageId:crypto.randomUUID()};pending.set(key,payload);}
    sending=true;input.disabled=true;sendButton.disabled=true;error.textContent='';
    try{
      await request(url,'POST',payload);pending.delete(key);drafts.delete(key);
      if(revision===generation){input.value='';count();await refreshMessages();timeline.scrollTop=timeline.scrollHeight;input.focus();}
      await inbox();
    }catch(e){if(revision===generation)error.textContent=(e.message||'Không nhận được phản hồi.')+' Nội dung được giữ lại để bạn gửi lại.';}
    finally{sending=false;input.disabled=admin&&!selected;sendButton.disabled=admin&&!selected;}
  });
  if(admin){$('[data-inbox-prev]').addEventListener('click',()=>{if(page){page--;inbox();}});$('[data-inbox-next]').addEventListener('click',()=>{if(page+1<totalPages){page++;inbox();}});}
  document.addEventListener('visibilitychange',()=>{if(!document.hidden){refreshMessages();inbox();unread();}});
  async function poll(){if(!document.hidden){await refreshMessages();await inbox();await unread();}setTimeout(poll,3000);}
  count();refreshMessages(true);inbox();unread();setTimeout(poll,3000);
})();
