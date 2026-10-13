package vn.techshop.chat;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import vn.techshop.identity.*;
import vn.techshop.shared.BusinessException;

@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class ChatService {
    public record Send(@NotBlank @Size(max=2000) String content,@NotNull UUID clientMessageId) {}
    public record Read(@NotNull @Positive Long upToId) {}
    public record AiStart(@AssertTrue(message="Bạn cần đồng ý gửi câu hỏi tới Gemini để bật AI.") boolean consent) {}
    public record Support(boolean available,String supportMode,String handoffStatus,String aiStatus) {}
    public record Message(Long id,String senderRole,String senderName,String content,Instant createdAt,Long inReplyToId,List<vn.techshop.ai.AiAdvisor.Card> products) {}
    public record History(List<Message> messages,boolean hasMore,long customerReadId,long adminReadId,Support support) {}
    public record Conversation(Long id,Long customerId,String customerName,String customerEmail,String lastMessage,Instant updatedAt,long unreadCount,String supportMode,String handoffStatus,String aiStatus) {}
    public record Inbox(List<Conversation> items,int page,int totalPages,long totalElements) {}
    public record Unread(long unreadCount) {}
    private final ChatConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final UserRepository users;
    private final RequestLimits limits;
    private final Clock clock;
    private final vn.techshop.ai.AiSettings aiSettings;
    private final AiChatLifecycle ai;
    private final vn.techshop.ai.AiUsage usage;
    private final tools.jackson.databind.json.JsonMapper json=tools.jackson.databind.json.JsonMapper.builder().build();
    public ChatService(ChatConversationRepository conversations,ChatMessageRepository messages,UserRepository users,RequestLimits limits,Clock clock,vn.techshop.ai.AiSettings aiSettings,AiChatLifecycle ai,vn.techshop.ai.AiUsage usage) {
        this.conversations=conversations;this.messages=messages;this.users=users;this.limits=limits;this.clock=clock;this.aiSettings=aiSettings;this.ai=ai;this.usage=usage;
    }
    private AppUser actor(TechPrincipal principal,String role) {
        if(principal==null || !role.equals(principal.role())) throw new BusinessException(HttpStatus.FORBIDDEN,"ACCESS_DENIED","Bạn không có quyền sử dụng cuộc trò chuyện này.");
        var user=users.findById(principal.id()).orElseThrow(BusinessException::missing);
        if(!role.equals(user.getRole()) || user.isGuestAccount()) throw new BusinessException(HttpStatus.FORBIDDEN,"ACCESS_DENIED","Vui lòng đăng nhập bằng tài khoản khách hàng hoặc quản trị viên.");
        return user;
    }
    private ChatConversation adminConversation(TechPrincipal principal,Long id,boolean lock) {
        actor(principal,"ADMIN");
        return (lock?conversations.lockById(id):conversations.findById(id)).orElseThrow(BusinessException::missing);
    }
    @Transactional(readOnly=true)
    public Inbox inbox(TechPrincipal principal,int page) {
        actor(principal,"ADMIN");
        if(page<0 || page>10000) throw BusinessException.invalid("Trang không hợp lệ.");
        var result=conversations.inbox(PageRequest.of(page,30));
        return new Inbox(result.getContent().stream().map(c->new Conversation(c.getId(),c.getCustomerId(),c.getCustomerName(),c.getCustomerEmail(),c.getLastMessage(),c.getUpdatedAt(),c.getUnreadCount(),c.getSupportMode(),c.getHandoffStatus(),c.getAiStatus())).toList(),page,result.getTotalPages(),result.getTotalElements());
    }
    @Transactional(readOnly=true)
    public History customerHistory(TechPrincipal principal,Long afterId,Long beforeId) {
        actor(principal,"CUSTOMER");validateCursor(afterId,beforeId);
        return conversations.findByCustomerId(principal.id()).map(c->history(c,afterId,beforeId)).orElseGet(()->new History(List.of(),false,0,0,new Support(aiSettings.ready(),"ADMIN","NONE","IDLE")));
    }
    @Transactional(readOnly=true)
    public History adminHistory(TechPrincipal principal,Long id,Long afterId,Long beforeId) {
        validateCursor(afterId,beforeId);return history(adminConversation(principal,id,false),afterId,beforeId);
    }
    private void validateCursor(Long afterId,Long beforeId) {
        if(afterId!=null && beforeId!=null || afterId!=null && afterId<0 || beforeId!=null && beforeId<=0) throw BusinessException.invalid("Mốc tin nhắn không hợp lệ.");
    }
    private History history(ChatConversation c,Long afterId,Long beforeId) {
        var page=PageRequest.of(0,51);
        var rows=new ArrayList<>(afterId!=null?messages.findByConversationIdAndIdGreaterThanOrderByIdAsc(c.id,afterId,page):beforeId!=null?messages.findByConversationIdAndIdLessThanOrderByIdDesc(c.id,beforeId,page):messages.findByConversationIdOrderByIdDesc(c.id,page));
        boolean more=rows.size()>50;if(more)rows.removeLast();
        if(afterId==null)Collections.reverse(rows);
        return new History(rows.stream().map(this::view).toList(),more,c.customerReadId,c.adminReadId,support(c));
    }
    public Message customerSend(TechPrincipal principal,Send input) {
        var sender=actor(principal,"CUSTOMER");
        // Serialise first-message creation on the existing customer, avoiding duplicate conversations.
        users.lockById(sender.getId()).orElseThrow(BusinessException::missing);
        var c=conversations.findByCustomerId(sender.getId()).orElseGet(()->{
            var created=new ChatConversation();created.customerId=sender.getId();created.lastMessage="";created.updatedAt=clock.instant();return conversations.saveAndFlush(created);
        });
        return send(conversations.lockById(c.id).orElseThrow(),sender,input);
    }
    public Message adminSend(TechPrincipal principal,Long id,Send input) {
        var c=adminConversation(principal,id,true);ai.accept(c,principal.id());return send(c,users.findById(principal.id()).orElseThrow(),input);
    }
    private Message send(ChatConversation c,AppUser sender,Send input) {
        if(input.content()==null || input.content().isBlank() || input.content().length()>2000 || input.clientMessageId()==null) throw BusinessException.invalid("Tin nhắn cần có nội dung và tối đa 2.000 ký tự.");
        var content=input.content().strip();
        var existing=messages.findByConversationIdAndSenderIdAndClientMessageId(c.id,sender.getId(),input.clientMessageId().toString());
        if(existing.isPresent()) {
            if(!existing.get().content.equals(content)) throw BusinessException.conflict("CHAT_KEY_REUSED","Mã tin nhắn đã được dùng cho nội dung khác.");
            return view(existing.get());
        }
        limits.consume("chat-send:"+sender.getId(),30,Duration.ofMinutes(1));
        var m=new ChatMessage();m.conversationId=c.id;m.senderId=sender.getId();m.senderRole=sender.getRole();m.senderName=sender.getFullName();m.content=content;m.clientMessageId=input.clientMessageId().toString();m.createdAt=clock.instant();
        messages.saveAndFlush(m);c.lastMessage=content;c.updatedAt=m.createdAt;
        if(sender.getRole().equals("CUSTOMER"))ai.enqueue(c,m);
        return view(m);
    }
    public void customerRead(TechPrincipal principal,Read input) {
        actor(principal,"CUSTOMER");
        var c=conversations.findByCustomerId(principal.id()).orElseThrow(BusinessException::missing);
        mark(conversations.lockById(c.id).orElseThrow(),input,false);
    }
    public void adminRead(TechPrincipal principal,Long id,Read input) {mark(adminConversation(principal,id,true),input,true);}
    private void mark(ChatConversation c,Read input,boolean admin) {
        if(input.upToId()==null || !messages.existsByIdAndConversationId(input.upToId(),c.id)) throw BusinessException.missing();
        // Only acknowledge what the browser received; messages arriving concurrently remain unread.
        if(admin)c.adminReadId=Math.max(c.adminReadId,input.upToId());else c.customerReadId=Math.max(c.customerReadId,input.upToId());
    }
    @Transactional(readOnly=true)
    public Unread unread(TechPrincipal principal,boolean admin) {
        actor(principal,admin?"ADMIN":"CUSTOMER");return new Unread(admin?messages.adminUnread():messages.customerUnread(principal.id()));
    }
    private Support support(ChatConversation c){return new Support(aiSettings.ready(),c.supportMode,c.handoffStatus,c.aiStatus);}
    private ChatConversation ownLocked(TechPrincipal principal,boolean create){
        actor(principal,"CUSTOMER");users.lockById(principal.id()).orElseThrow(BusinessException::missing);
        var c=conversations.findByCustomerId(principal.id()).orElseGet(()->{
            if(!create)throw BusinessException.missing();var n=new ChatConversation();n.customerId=principal.id();n.lastMessage="";n.updatedAt=clock.instant();return conversations.saveAndFlush(n);
        });return conversations.lockById(c.id).orElseThrow();
    }
    public Support startAi(TechPrincipal user,AiStart input){
        actor(user,"CUSTOMER");
        if(!input.consent())throw BusinessException.invalid("Vui lòng đồng ý gửi câu hỏi tới Gemini.");
        if(!aiSettings.ready())throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"AI_NOT_CONFIGURED","Trợ lý AI chưa được cấu hình. Bạn có thể chat với nhân viên.");
        var c=ownLocked(user,true);
        if(c.handoffStatus.equals("ACCEPTED"))throw BusinessException.conflict("ADMIN_SUPPORT_ACTIVE","Nhân viên đang hỗ trợ bạn. Hãy chờ nhân viên kết thúc trước khi bật AI lại.");
        if(!c.supportMode.equals("AI")){ai.invalidate(c);c.supportMode="AI";c.handoffStatus="NONE";c.acceptedAdminId=null;c.aiStartMessageId=messages.latestId(c.id);}
        return support(c);
    }
    public Support handoff(TechPrincipal user){var c=ownLocked(user,true);if(!c.supportMode.equals("ADMIN")||!c.handoffStatus.equals("ACCEPTED")){if(!c.handoffStatus.equals("WAITING"))ai.handoff(c);}return support(c);}
    public Support accept(TechPrincipal user,Long id){var c=adminConversation(user,id,true);if(!c.handoffStatus.equals("ACCEPTED"))ai.accept(c,user.id());return support(c);}
    public Support closeSupport(TechPrincipal user,Long id){var c=adminConversation(user,id,true);ai.invalidate(c);c.supportMode="ADMIN";c.handoffStatus="CLOSED";c.acceptedAdminId=null;return support(c);}
    @Transactional(readOnly=true) public Map<String,Object> aiStatus(TechPrincipal user){actor(user,"ADMIN");return usage.status();}
    private Message view(ChatMessage m) {
        List<vn.techshop.ai.AiAdvisor.Card> cards=m.productsJson==null?List.of():Arrays.asList(json.readValue(m.productsJson,vn.techshop.ai.AiAdvisor.Card[].class));
        return new Message(m.id,m.senderRole,m.senderName,m.content,m.createdAt,m.inReplyToId,cards);
    }
}
