package vn.techshop.chat;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.techshop.identity.*;
import vn.techshop.shared.BusinessException;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ChatIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired IdentityService identity;
    @Autowired UserRepository users;
    @Autowired ChatService chat;
    @Autowired ChatConversationRepository conversations;
    @Autowired ChatMessageRepository messages;
    @Autowired JdbcTemplate jdbc;
    TechPrincipal customer,other,admin;
    @BeforeEach void fixture(){
        var key=UUID.randomUUID().toString();
        customer=principal(identity.register(new IdentityService.Registration("Khách chat","chat-"+key+"@test.local","ChatTest2026!")).id());
        other=principal(identity.register(new IdentityService.Registration("Khách khác","other-chat-"+key+"@test.local","ChatTest2026!")).id());
        admin=principal(identity.bootstrapAdmin(new IdentityService.Registration("Admin chat","admin-chat-"+key+"@test.local","ChatTest2026!")).id());
    }
    TechPrincipal principal(Long id){return new TechPrincipal(users.findById(id).orElseThrow());}
    ChatService.Send send(String content){return new ChatService.Send(content,UUID.randomUUID());}
    Long conversation(){return conversations.findByCustomerId(customer.id()).orElseThrow().id;}
    @Test void twoWayConversationUnreadAndReadReceipts() throws Exception {
        mvc.perform(get("/api/v1/chat/messages").with(user(customer))).andExpect(status().isOk()).andExpect(jsonPath("$.messages").isEmpty()).andExpect(header().string("Cache-Control","no-store"));
        var first=chat.customerSend(customer,send("Xin chào, tư vấn giúp tôi."));var id=conversation();
        mvc.perform(get("/api/v1/admin/chat/conversations").with(user(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.items[?(@.id == "+id+")].unreadCount").value(org.hamcrest.Matchers.contains(1)));
        chat.adminRead(admin,id,new ChatService.Read(first.id()));
        var reply=chat.adminSend(admin,id,send("TechZone chào bạn!"));
        mvc.perform(get("/api/v1/chat/unread").with(user(customer))).andExpect(jsonPath("$.unreadCount").value(1));
        mvc.perform(post("/api/v1/chat/read").with(user(customer)).with(csrf()).contentType("application/json").content("{\"upToId\":"+reply.id()+"}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/chat/unread").with(user(customer))).andExpect(jsonPath("$.unreadCount").value(0));
        mvc.perform(get("/api/v1/admin/chat/conversations/"+id+"/messages").with(user(admin))).andExpect(jsonPath("$.messages.length()").value(2)).andExpect(jsonPath("$.customerReadId").value(reply.id()));
        assertThat(chat.customerHistory(other,null,null).messages()).isEmpty();
    }
    @Test void authenticationRolesCsrfAndForgeryAreRejected() throws Exception {
        var first=chat.customerSend(customer,send("Tin riêng"));var id=conversation();
        mvc.perform(get("/api/v1/chat/messages")).andExpect(status().isUnauthorized());
        mvc.perform(get("/chat")).andExpect(status().isSeeOther());
        mvc.perform(get("/api/v1/chat/messages").with(user(admin))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/chat/conversations/"+id+"/messages").with(user(other))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/chat/messages").with(user(customer)).contentType("application/json").content("{\"content\":\"hi\",\"clientMessageId\":\""+UUID.randomUUID()+"\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/chat/messages").with(user(customer)).with(csrf()).contentType("application/json").content("{\"content\":\"hi\",\"clientMessageId\":\""+UUID.randomUUID()+"\",\"senderRole\":\"ADMIN\"}")).andExpect(status().isBadRequest());
        // A customer cannot acknowledge an ID from another customer's conversation.
        chat.customerSend(other,send("Cuộc trò chuyện khác"));
        mvc.perform(post("/api/v1/chat/read").with(user(other)).with(csrf()).contentType("application/json").content("{\"upToId\":"+first.id()+"}")).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/admin/chat/conversations/"+id+"/read").with(user(admin)).contentType("application/json").content("{\"upToId\":"+first.id()+"}")).andExpect(status().isForbidden());
    }
    @Test void validationAndUnknownConversationsReturnControlledErrors() throws Exception {
        for(var body:List.of("{\"content\":\"   \" ,\"clientMessageId\":\""+UUID.randomUUID()+"\"}","{\"content\":\""+"a".repeat(2001)+"\",\"clientMessageId\":\""+UUID.randomUUID()+"\"}","{\"content\":\"hi\"}"))
            mvc.perform(post("/api/v1/chat/messages").with(user(customer)).with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/chat/messages?afterId=-1").with(user(customer))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/chat/messages?afterId=1&beforeId=2").with(user(customer))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/chat/conversations/9223372036854775807/messages").with(user(admin))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/admin/chat/conversations?page=-1").with(user(admin))).andExpect(status().isBadRequest());
    }
    @Test void retriesAreIdempotentAndSenderIdentityComesFromSession() throws Exception {
        var input=send("  <script>alert('x')</script> \n hỏi về đơn hàng  ");
        var first=chat.customerSend(customer,input);var replay=chat.customerSend(customer,input);
        assertThat(replay.id()).isEqualTo(first.id());assertThat(replay.senderRole()).isEqualTo("CUSTOMER");assertThat(replay.senderName()).isEqualTo("Khách chat");
        assertThat(chat.customerHistory(customer,null,null).messages()).hasSize(1);
        assertThatThrownBy(()->chat.customerSend(customer,new ChatService.Send("different",input.clientMessageId()))).isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.code()).isEqualTo("CHAT_KEY_REUSED"));
        mvc.perform(get("/api/v1/chat/messages").with(user(customer))).andExpect(jsonPath("$.messages[0].content").value(input.content().strip())).andExpect(jsonPath("$.messages[0].senderId").doesNotExist());
    }
    @Test void readWatermarkDoesNotHideLaterMessagesOrMoveBackwards() {
        var first=chat.customerSend(customer,send("1"));var id=conversation();
        var second=chat.customerSend(customer,send("2"));
        chat.adminRead(admin,id,new ChatService.Read(first.id()));
        assertThat(conversations.inbox(org.springframework.data.domain.PageRequest.of(0,100)).stream().filter(c->c.getId().equals(id)).findFirst().orElseThrow().getUnreadCount()).isEqualTo(1);
        chat.adminRead(admin,id,new ChatService.Read(second.id()));chat.adminRead(admin,id,new ChatService.Read(first.id()));
        assertThat(chat.adminHistory(admin,id,null,null).adminReadId()).isEqualTo(second.id());
    }
    @Test void cursorPagingHasNoGapsOrDuplicateMessages() {
        chat.customerSend(customer,send("First"));var id=conversation();
        for(int i=0;i<105;i++)jdbc.update("insert into chat_messages(conversation_id,sender_id,sender_role,sender_name,content,client_message_id,created_at) values (?,?,'CUSTOMER','Khách chat',?,?,CURRENT_TIMESTAMP)",id,customer.id(),"message "+i,UUID.randomUUID().toString());
        var latest=chat.customerHistory(customer,null,null);assertThat(latest.messages()).hasSize(50);assertThat(latest.hasMore()).isTrue();
        var previous=chat.customerHistory(customer,null,latest.messages().getFirst().id());assertThat(previous.messages()).hasSize(50);
        var oldest=chat.customerHistory(customer,null,previous.messages().getFirst().id());assertThat(oldest.messages()).hasSize(6);assertThat(oldest.hasMore()).isFalse();
        var all=new ArrayList<ChatService.Message>();all.addAll(oldest.messages());all.addAll(previous.messages());all.addAll(latest.messages());
        assertThat(all.stream().map(ChatService.Message::id).toList()).doesNotHaveDuplicates().isSorted();
        var incremental=chat.customerHistory(customer,all.getFirst().id(),null);assertThat(incremental.messages()).hasSize(50);assertThat(incremental.hasMore()).isTrue();
        assertThat(chat.customerHistory(customer,all.getLast().id(),null).messages()).isEmpty();
    }
    @Test void concurrentFirstMessagesCreateOneConversationAndConcurrentRetryCreatesOneMessage() throws Exception {
        var pool=Executors.newFixedThreadPool(2);
        try {
            var start=new CountDownLatch(1);var input=send("Concurrent retry");
            var tasks=new ArrayList<Future<ChatService.Message>>();
            for(int i=0;i<2;i++)tasks.add(pool.submit(()->{start.await();return chat.customerSend(customer,input);}));
            start.countDown();assertThat(tasks.get(0).get(15,TimeUnit.SECONDS).id()).isEqualTo(tasks.get(1).get(15,TimeUnit.SECONDS).id());
            assertThat(chat.customerHistory(customer,null,null).messages()).hasSize(1);
            assertThat(jdbc.queryForObject("select count(*) from chat_conversations where customer_id=?",Integer.class,customer.id())).isEqualTo(1);
        }finally{pool.shutdownNow();}
    }
    @Test void pagesRenderWithChatNavigationAndGuestAccountsCannotUseChat() throws Exception {
        mvc.perform(get("/chat").with(user(customer))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("data-admin=\"false\""))).andExpect(content().string(org.hamcrest.Matchers.containsString("/js/chat.js"))).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("class=\"admin-nav\""))));
        mvc.perform(get("/admin/chat").with(user(admin))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("data-admin=\"true\"")));
        var guest=users.findById(other.id()).orElseThrow();guest.setGuestAccount(true);users.saveAndFlush(guest);
        mvc.perform(get("/api/v1/chat/messages").with(user(principal(guest.getId())))).andExpect(status().isForbidden());
    }
    @Test void sendRateLimitRejectsExcessAndAllowsReplay() throws Exception {
        var first=send("rate 0");chat.customerSend(customer,first);
        for(int i=1;i<30;i++)chat.customerSend(customer,send("rate "+i));
        assertThat(chat.customerSend(customer,first).content()).isEqualTo("rate 0");
        mvc.perform(post("/api/v1/chat/messages").with(user(customer)).with(csrf()).contentType("application/json").content("{\"content\":\"excess\",\"clientMessageId\":\""+UUID.randomUUID()+"\"}")).andExpect(status().isTooManyRequests());
        assertThat(messages.findByConversationIdOrderByIdDesc(conversation(),org.springframework.data.domain.PageRequest.of(0,100))).hasSize(30);
    }
}
