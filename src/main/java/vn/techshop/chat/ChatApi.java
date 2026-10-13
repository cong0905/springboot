package vn.techshop.chat;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.techshop.identity.TechPrincipal;

@RestController
public class ChatApi {
    private final ChatService chat;
    public ChatApi(ChatService chat){this.chat=chat;}
    @GetMapping("/api/v1/chat/messages")
    ChatService.History customerMessages(@AuthenticationPrincipal TechPrincipal user,@RequestParam(required=false) Long afterId,@RequestParam(required=false) Long beforeId){return chat.customerHistory(user,afterId,beforeId);}
    @PostMapping("/api/v1/chat/messages") @ResponseStatus(HttpStatus.CREATED)
    ChatService.Message customerSend(@AuthenticationPrincipal TechPrincipal user,@Valid @RequestBody ChatService.Send input){return chat.customerSend(user,input);}
    @PostMapping("/api/v1/chat/read") @ResponseStatus(HttpStatus.NO_CONTENT)
    void customerRead(@AuthenticationPrincipal TechPrincipal user,@Valid @RequestBody ChatService.Read input){chat.customerRead(user,input);}
    @GetMapping("/api/v1/chat/unread")
    ChatService.Unread customerUnread(@AuthenticationPrincipal TechPrincipal user){return chat.unread(user,false);}
    @GetMapping("/api/v1/admin/chat/conversations")
    ChatService.Inbox inbox(@AuthenticationPrincipal TechPrincipal user,@RequestParam(defaultValue="0") int page){return chat.inbox(user,page);}
    @GetMapping("/api/v1/admin/chat/conversations/{id}/messages")
    ChatService.History adminMessages(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,@RequestParam(required=false) Long afterId,@RequestParam(required=false) Long beforeId){return chat.adminHistory(user,id,afterId,beforeId);}
    @PostMapping("/api/v1/admin/chat/conversations/{id}/messages") @ResponseStatus(HttpStatus.CREATED)
    ChatService.Message adminSend(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,@Valid @RequestBody ChatService.Send input){return chat.adminSend(user,id,input);}
    @PostMapping("/api/v1/admin/chat/conversations/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT)
    void adminRead(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,@Valid @RequestBody ChatService.Read input){chat.adminRead(user,id,input);}
    @GetMapping("/api/v1/admin/chat/unread")
    ChatService.Unread adminUnread(@AuthenticationPrincipal TechPrincipal user){return chat.unread(user,true);}
    @PostMapping("/api/v1/chat/ai/start")
    ChatService.Support start(@AuthenticationPrincipal TechPrincipal user,@Valid @RequestBody ChatService.AiStart input){return chat.startAi(user,input);}
    @PostMapping("/api/v1/chat/handoff")
    ChatService.Support handoff(@AuthenticationPrincipal TechPrincipal user){return chat.handoff(user);}
    @PostMapping("/api/v1/admin/chat/conversations/{id}/accept")
    ChatService.Support accept(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id){return chat.accept(user,id);}
    @PostMapping("/api/v1/admin/chat/conversations/{id}/close-support")
    ChatService.Support closeSupport(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id){return chat.closeSupport(user,id);}
    @GetMapping("/api/v1/admin/chat/ai/status")
    java.util.Map<String,Object> aiStatus(@AuthenticationPrincipal TechPrincipal user){return chat.aiStatus(user);}
    @ModelAttribute void privateResponse(jakarta.servlet.http.HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
}
