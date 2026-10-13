package vn.techshop.chat;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;
import vn.techshop.ai.*;
import vn.techshop.catalog.*;
import vn.techshop.identity.*;
import vn.techshop.inventory.*;
import vn.techshop.shared.BusinessException;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

/** Real application/database pipeline; only the external Gemini gateway is fake. No real API key/calls. */
@SpringBootTest(properties={"app.ai.enabled=true","app.ai.api-key=fake-test-only-never-real","app.ai.model=test-model","app.ai.worker-enabled=false"})
@AutoConfigureMockMvc @ActiveProfiles("test") @Import(AiChatIntegrationTest.Fakes.class) @DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class AiChatIntegrationTest {
 @TestConfiguration static class Fakes { @Bean @Primary FakeGateway fakeGateway(){return new FakeGateway();} }
 static class FakeGateway implements GeminiGateway {
  int calls;Long product;String keyword;String kind="CATALOG_SEARCH";String answer="[Giả lập] Mình gợi ý thiết bị dưới đây. Hãy xem phiên bản phù hợp.";String forcedJson;boolean fail;boolean transientFailure;CountDownLatch entered,release;final List<String> inputs=new ArrayList<>();
  void reset(){calls=0;kind="CATALOG_SEARCH";forcedJson=null;fail=false;transientFailure=false;entered=null;release=null;inputs.clear();}
  public synchronized Result generate(String instructions,String input,Map<String,Object> schema,Instant deadline){
   calls++;inputs.add(input);if(entered!=null){entered.countDown();try{if(!release.await(10,TimeUnit.SECONDS))throw new IllegalStateException("test latch");}catch(InterruptedException e){throw new IllegalStateException(e);}entered=null;}
   if(fail)throw new Failure("PROVIDER_UNAVAILABLE",transientFailure);
   if(schema.toString().contains("CATALOG_SEARCH"))return new Result("{\"intent\":\""+kind+"\",\"keyword\":\""+keyword+"\",\"categoryId\":null,\"minPrice\":null,\"maxPrice\":null,\"brand\":null,\"storage\":null}",10,5);
   return new Result(forcedJson!=null?forcedJson:"{\"answer\":\""+answer+"\",\"productIds\":["+product+"],\"handoffSuggested\":false}",20,8);
  }
 }
 @Autowired ChatService chat;@Autowired AiChatWorker worker;@Autowired ChatConversationRepository conversations;@Autowired ChatMessageRepository messages;@Autowired AiJobRepository jobs;
 @Autowired IdentityService identity;@Autowired UserRepository users;@Autowired CatalogService catalog;@Autowired InventoryService inventory;@Autowired JdbcTemplate jdbc;@Autowired FakeGateway fake;@Autowired MockMvc mvc;@Autowired ConfigurableEnvironment environment;
 TechPrincipal customer,admin,other;Long product;Map<String,Object> settings;
 @BeforeEach void fixture(){
  jobs.deleteAll();jdbc.update("delete from ai_usage_limits");fake.reset();settings=new HashMap<>();environment.getPropertySources().addFirst(new MapPropertySource("ai-test-overrides",settings));
  String key=UUID.randomUUID().toString().substring(0,8);
  customer=principal(identity.register(new IdentityService.Registration("Khách AI","ai-"+key+"@test.local","AiTest2026!")).id());
  other=principal(identity.register(new IdentityService.Registration("Khách AI khác","ai-other-"+key+"@test.local","AiTest2026!")).id());
  admin=principal(identity.bootstrapAdmin(new IdentityService.Registration("Admin AI","ai-admin-"+key+"@test.local","AiTest2026!")).id());
  var category=catalog.createCategory(new CatalogService.CategoryInput("AI"+key,"Thiết bị AI "+key,"ACTIVE"));
  var p=catalog.createProduct(new CatalogService.ProductInput(category.id(),"AIP"+key,"Thiết bị "+key,new BigDecimal("5000000"),"ACTIVE","INTERNAL_DESCRIPTION_DO_NOT_SEND"));
  inventory.adjust(p.id(),admin.id(),new InventoryService.Adjustment(4,"AI test stock",p.version()));product=p.id();fake.product=product;fake.keyword=key;
 }
 @AfterEach void restore(){environment.getPropertySources().remove("ai-test-overrides");}
 TechPrincipal principal(Long id){return new TechPrincipal(users.findById(id).orElseThrow());}
 ChatService.Send send(String content){return new ChatService.Send(content,UUID.randomUUID());}
 Long id(){return conversations.findByCustomerId(customer.id()).orElseThrow().id;}
 void start(){chat.startAi(customer,new ChatService.AiStart(true));}
 ChatService.Message ask(){return chat.customerSend(customer,send("Tư vấn thiết bị giúp tôi"));}
 int aiCount(){return jdbc.queryForObject("select count(*) from chat_messages where conversation_id=? and sender_role='AI'",Integer.class,id());}
 String mode(){return chat.customerHistory(customer,null,null).support().supportMode();}
 boolean runWorker(){boolean ran=worker.runOnce();if(!ran && !jobs.findAll().isEmpty())System.out.println("AI_TEST_QUEUE_DIAGNOSTIC now="+Instant.now()+" jobs="+jobs.findAll().stream().map(j->j.id+":"+j.status+":"+j.nextAttemptAt+":"+j.leaseUntil+":"+j.attempts).toList());return ran;}
 @Test void consentAvailabilityCsrfAndRoles() throws Exception {
  mvc.perform(post("/api/v1/chat/ai/start").with(user(customer)).with(csrf()).contentType("application/json").content("{\"consent\":false}")).andExpect(status().isBadRequest());
  mvc.perform(post("/api/v1/chat/ai/start").with(user(customer)).contentType("application/json").content("{\"consent\":true}")).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/chat/ai/start").with(user(admin)).with(csrf()).contentType("application/json").content("{\"consent\":true}")).andExpect(status().isForbidden());
  settings.put("app.ai.api-key","");mvc.perform(post("/api/v1/chat/ai/start").with(user(customer)).with(csrf()).contentType("application/json").content("{\"consent\":true}")).andExpect(status().isServiceUnavailable());
  ask();assertThat(runWorker()).isFalse();assertThat(fake.calls).isZero();assertThat(mode()).isEqualTo("ADMIN");
  mvc.perform(get("/api/v1/admin/chat/ai/status").with(user(other))).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/admin/chat/ai/status").with(user(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.apiKey").doesNotExist());
 }
 @Test void groundedReplyCardsTokensUnreadAndIdempotency() throws Exception {
  start();var input=send("Tư vấn thiết bị, email abc@example.com, số 0912345678");var first=chat.customerSend(customer,input);assertThat(chat.customerSend(customer,input).id()).isEqualTo(first.id());assertThat(jobs.count()).isEqualTo(1);
  assertThat(runWorker()).isTrue();assertThat(runWorker()).isFalse();assertThat(fake.calls).isEqualTo(2);assertThat(aiCount()).isEqualTo(1);
  var reply=chat.customerHistory(customer,null,null).messages().getLast();assertThat(reply.senderRole()).isEqualTo("AI");assertThat(reply.inReplyToId()).isEqualTo(first.id());assertThat(reply.products()).hasSize(1);assertThat(reply.products().getFirst().price()).isEqualByComparingTo("5000000");assertThat(reply.products().getFirst().available()).isEqualTo(4);
  assertThat(fake.inputs.toString()).doesNotContain("abc@example.com","0912345678","INTERNAL_DESCRIPTION_DO_NOT_SEND","@test.local");assertThat(chat.unread(customer,false).unreadCount()).isEqualTo(1);assertThat(conversations.inbox(org.springframework.data.domain.PageRequest.of(0,1000)).stream().filter(c->c.getId().equals(id())).findFirst().orElseThrow().getUnreadCount()).isZero();
  assertThat(jdbc.queryForObject("select input_tokens+output_tokens from ai_chat_jobs where source_message_id=?",Integer.class,first.id())).isEqualTo(43);
  mvc.perform(get("/api/v1/chat/messages").with(user(customer))).andExpect(jsonPath("$.messages[1].products[0].url").value("/products/"+product)).andExpect(jsonPath("$.messages[1].products[0].retrievedAt").isNotEmpty());assertThat(chat.customerHistory(other,null,null).messages()).isEmpty();
 }
 @Test void adminAcceptAndHandoffCancelQueuedAiAndActiveSupportCannotRestart() throws Exception {
  start();ask();chat.accept(admin,id());assertThat(runWorker()).isFalse();assertThat(aiCount()).isZero();assertThatThrownBy(this::start).isInstanceOf(BusinessException.class);
  chat.adminSend(admin,id(),send("Nhân viên đã tiếp nhận"));chat.closeSupport(admin,id());start();ask();chat.handoff(customer);assertThat(runWorker()).isFalse();assertThat(mode()).isEqualTo("ADMIN");assertThat(fake.calls).isZero();
  mvc.perform(post("/api/v1/admin/chat/conversations/"+id()+"/accept").with(user(other)).with(csrf())).andExpect(status().isForbidden());
 }
 @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans={false,true}) void inFlightResultIsDiscardedAfterHandoffWithoutHoldingDatabaseLock(boolean adminAccept) throws Exception {
  start();ask();fake.entered=new CountDownLatch(1);fake.release=new CountDownLatch(1);var pool=Executors.newSingleThreadExecutor();
  try{var work=pool.submit(this::runWorker);assertThat(fake.entered.await(5,TimeUnit.SECONDS)).isTrue();var begin=Instant.now();if(adminAccept)chat.accept(admin,id());else chat.handoff(customer);assertThat(Duration.between(begin,Instant.now()).toMillis()).isLessThan(3000);fake.release.countDown();assertThat(work.get(10,TimeUnit.SECONDS)).isTrue();assertThat(aiCount()).isZero();assertThat(fake.calls).isEqualTo(1);assertThat(jobs.findAll().getFirst().status).isEqualTo("SUPERSEDED");}finally{fake.release.countDown();pool.shutdownNow();}
 }
 @Test void disabledAiCancelsPersistedQueueWithoutProviderCall(){start();ask();settings.put("app.ai.enabled","false");assertThat(runWorker()).isFalse();assertThat(mode()).isEqualTo("ADMIN");assertThat(jobs.findAll().getFirst().status).isEqualTo("SUPERSEDED");assertThat(fake.calls).isZero();}
 @Test void privateRequestsGoToStaffWithoutExternalCall(){start();chat.customerSend(customer,send("Kiểm tra đơn hàng và địa chỉ nhà giúp tôi"));assertThat(mode()).isEqualTo("ADMIN");assertThat(jobs.count()).isZero();assertThat(fake.calls).isZero();}
 @Test void invalidProductAndMalformedOutputNeverPublish(){start();ask();fake.forcedJson="{\"answer\":\"Sản phẩm sai\",\"productIds\":[999999999],\"handoffSuggested\":false}";runWorker();assertThat(aiCount()).isZero();assertThat(mode()).isEqualTo("ADMIN");assertThat(jobs.findAll().getFirst().errorCode).isEqualTo("INVALID_PRODUCT_ID");}
 @Test void malformedJsonRoutesToStaff(){start();ask();fake.forcedJson="not json";runWorker();assertThat(aiCount()).isZero();assertThat(mode()).isEqualTo("ADMIN");}
 @Test void missingStockOrInactiveCatalogCannotBeRecommended(){start();ask();jdbc.update("update products set stock_quantity=0 where id=?",product);runWorker();assertThat(aiCount()).isZero();assertThat(mode()).isEqualTo("ADMIN");assertThat(fake.inputs.getLast()).contains("Sản phẩm công khai: []");}
 @Test void userQuotaPreservesMessagesAndHandsOff(){settings.put("app.ai.user-turns-per-minute","1");start();ask();chat.customerSend(customer,send("Tôi cần thêm tư vấn"));assertThat(chat.customerHistory(customer,null,null).messages()).hasSize(2);assertThat(mode()).isEqualTo("ADMIN");assertThat(runWorker()).isFalse();assertThat(fake.calls).isZero();assertThat(jdbc.queryForObject("select usage_count from ai_usage_limits where usage_key like ?",Integer.class,"user:"+customer.id()+":m:%")).isEqualTo(1);}
 @Test void globalQuotaStopsProviderCallsAndRetainsHumanChat(){settings.put("app.ai.global-requests-per-minute","1");start();ask();runWorker();assertThat(fake.calls).isEqualTo(1);assertThat(aiCount()).isZero();assertThat(mode()).isEqualTo("ADMIN");assertThat(chat.customerHistory(customer,null,null).messages()).hasSize(1);}
 @Test void transientRetryIsBoundedAndPersistsOriginalDeadline(){start();ask();fake.fail=true;fake.transientFailure=true;runWorker();var j=jobs.findAll().getFirst();assertThat(j.status).isEqualTo("QUEUED");var deadline=j.deadlineAt;j.nextAttemptAt=Instant.now().minusSeconds(1);jobs.saveAndFlush(j);assertThat(runWorker()).as("retry claim after deadline-controlled backoff").isTrue();j=jobs.findById(j.id).orElseThrow();assertThat(j.status).isEqualTo("FAILED");assertThat(j.attempts).isEqualTo(2);assertThat(j.deadlineAt).isEqualTo(deadline);assertThat(fake.calls).isEqualTo(2);assertThat(mode()).isEqualTo("ADMIN");}
 @Test void expiredLeaseRecoversAndExpiredDeadlineFallsBack(){start();ask();var j=jobs.findAll().getFirst();jdbc.update("update ai_chat_jobs set status='RUNNING',attempt_count=1,lease_token='lost-worker',lease_until=?,deadline_at=? where id=?",LocalDateTime.ofInstant(Instant.now().minusSeconds(5),ZoneOffset.UTC),LocalDateTime.ofInstant(Instant.now().plusSeconds(20),ZoneOffset.UTC),j.id);runWorker();assertThat(aiCount()).isEqualTo(1);assertThat(jobs.findById(j.id).orElseThrow().attempts).isEqualTo(2);
  chat.customerSend(customer,send("Câu hỏi tiếp theo"));var next=jobs.findAll().getLast();jdbc.update("update ai_chat_jobs set deadline_at=? where id=?",LocalDateTime.ofInstant(Instant.now().minusSeconds(2),ZoneOffset.UTC),next.id);runWorker();assertThat(mode()).isEqualTo("ADMIN");assertThat(fake.calls).isEqualTo(2);
 }
 @Test void newAiSessionExcludesOldHumanSupportHistory(){chat.customerSend(customer,send("OLD_CUSTOMER_PRIVATE_HISTORY"));chat.adminSend(admin,id(),send("OLD_ADMIN_PRIVATE_HISTORY"));chat.closeSupport(admin,id());start();ask();runWorker();assertThat(fake.inputs.toString()).doesNotContain("OLD_CUSTOMER_PRIVATE_HISTORY","OLD_ADMIN_PRIVATE_HISTORY");}
 @Test void handoffSuggestedByAiIsExplicitAndRetainsItsMessage(){start();ask();fake.kind="HANDOFF";runWorker();assertThat(aiCount()).isEqualTo(1);assertThat(mode()).isEqualTo("ADMIN");assertThat(chat.customerHistory(customer,null,null).support().handoffStatus()).isEqualTo("WAITING");assertThat(fake.calls).isEqualTo(1);}
 @Test void queuedTurnsRunInOrderAndReplyToTheirOwnSource(){start();var first=ask();var second=chat.customerSend(customer,send("Tư vấn thêm về thiết bị"));assertThat(runWorker()).isTrue();assertThat(chat.customerHistory(customer,null,null).support().aiStatus()).isEqualTo("QUEUED");assertThat(runWorker()).isTrue();var replies=chat.customerHistory(customer,null,null).messages().stream().filter(m->m.senderRole().equals("AI")).toList();assertThat(replies.stream().map(ChatService.Message::inReplyToId).toList()).containsExactly(first.id(),second.id());assertThat(chat.customerHistory(customer,null,null).support().aiStatus()).isEqualTo("IDLE");}
 @Test void generalSupportCanAskFollowUpWithoutInventingProduct(){start();ask();fake.kind="GENERAL_SUPPORT";fake.forcedJson="{\"answer\":\"[Giả lập] Bạn muốn mua loại thiết bị nào?\",\"productIds\":[],\"handoffSuggested\":false}";runWorker();assertThat(aiCount()).isEqualTo(1);assertThat(chat.customerHistory(customer,null,null).messages().getLast().products()).isEmpty();assertThat(mode()).isEqualTo("AI");}
 @Test void twoWorkersCannotPublishDuplicateReplies() throws Exception {start();ask();var pool=Executors.newFixedThreadPool(2);try{var start=new CountDownLatch(1);var a=pool.submit(()->{start.await();return runWorker();});var b=pool.submit(()->{start.await();return runWorker();});start.countDown();a.get(15,TimeUnit.SECONDS);b.get(15,TimeUnit.SECONDS);assertThat(aiCount()).isEqualTo(1);assertThat(fake.calls).isEqualTo(2);}finally{pool.shutdownNow();}}
}
