package vn.techshop.chat;

import jakarta.annotation.PreDestroy;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import vn.techshop.ai.*;

@Component
public class AiChatWorker {
 private record Work(Long id,Long conversationId,Long sourceId,long generation,String token,Instant deadline,List<Map<String,String>> history){}
 private final AiJobRepository jobs;private final ChatConversationRepository conversations;private final ChatMessageRepository messages;
 private final AiChatLifecycle lifecycle;private final AiAdvisor advisor;private final AiSettings settings;private final Clock clock;private final TransactionTemplate tx;
 @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
 private final ExecutorService pool;private final Semaphore slots;private final JsonMapper json=JsonMapper.builder().build();
 AiChatWorker(AiJobRepository jobs,ChatConversationRepository conversations,ChatMessageRepository messages,AiChatLifecycle lifecycle,AiAdvisor advisor,AiSettings settings,Clock clock,PlatformTransactionManager manager){
  this.jobs=jobs;this.conversations=conversations;this.messages=messages;this.lifecycle=lifecycle;this.advisor=advisor;this.settings=settings;this.clock=clock;tx=new TransactionTemplate(manager);tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
  slots=new Semaphore(settings.concurrency());pool=Executors.newFixedThreadPool(settings.concurrency());
 }
 @Scheduled(fixedDelay=1000) void tick(){
  if(!settings.workerEnabled())return;
  if(!settings.ready()){cancelUnavailable();return;}
  if(!slots.tryAcquire())return;
  try{pool.execute(()->{try{runOnce();}finally{slots.release();}});}catch(RejectedExecutionException e){slots.release();}
 }
 @PreDestroy void stop(){pool.shutdownNow();}
 private void cancelUnavailable(){tx.executeWithoutResult(s->{for(Long id:conversations.aiConversations(PageRequest.of(0,30))){var c=conversations.lockById(id).orElseThrow();if(c.supportMode.equals("AI")&&!settings.ready())lifecycle.handoff(c);}});}
 public boolean runOnce(){
  if(!settings.ready()){cancelUnavailable();return false;}
  var work=tx.execute(s->claim());if(work==null)return false;
  try{var reply=advisor.advise(work.history,work.deadline,()->current(work));tx.executeWithoutResult(s->complete(work,reply));}
  catch(GeminiGateway.Failure e){tx.executeWithoutResult(s->fail(work,e.code(),e.retryable()));}
  catch(Exception e){tx.executeWithoutResult(s->fail(work,"PROCESSING_FAILED",false));}
  return true;
 }
 private Work claim(){
  Instant now=clock.instant();
  for(var candidate:jobs.eligible(now,PageRequest.of(0,30))){
   var c=conversations.lockById(candidate.conversationId).orElse(null);if(c==null)continue;
   var j=jobs.findById(candidate.id).orElseThrow();entityManager.refresh(j);
   if(!c.supportMode.equals("AI")||c.aiGeneration!=j.generation){j.status="SUPERSEDED";continue;}
   if(!j.status.equals("QUEUED") && !(j.status.equals("RUNNING")&&j.leaseUntil.isBefore(now)))continue;
   if(c.aiLeaseUntil!=null && c.aiLeaseUntil.isAfter(now) || jobs.earlier(c.id,j.id)>0)continue;
   if(j.attempts>=2){j.status="FAILED";j.errorCode="RECOVERY_EXHAUSTED";lifecycle.handoff(c);c.aiStatus="FAILED";continue;}
   if(j.deadlineAt==null)j.deadlineAt=now.plusSeconds(settings.timeoutSeconds());
   if(!j.deadlineAt.isAfter(now)){j.status="FAILED";j.errorCode="TIMEOUT";lifecycle.handoff(c);c.aiStatus="FAILED";continue;}
   j.status="RUNNING";j.attempts++;j.leaseToken=UUID.randomUUID().toString();j.leaseUntil=j.deadlineAt.plusSeconds(15);j.model=settings.model();c.aiStatus="PROCESSING";c.aiCurrentJob=j.id;c.aiLeaseUntil=j.leaseUntil;
   var rows=messages.context(c.id,c.aiStartMessageId,j.sourceMessageId,PageRequest.of(0,12));var history=new ArrayList<Map<String,String>>();
   for(int i=rows.size()-1;i>=0;i--){var m=rows.get(i);history.add(Map.of("role",m.senderRole,"content",AiAdvisor.mask(m.content)));}
   return new Work(j.id,c.id,j.sourceMessageId,j.generation,j.leaseToken,j.deadlineAt,history);
  }return null;
 }
 private boolean current(Work w){return Boolean.TRUE.equals(tx.execute(s->{var c=conversations.findById(w.conversationId).orElse(null);var j=jobs.findById(w.id).orElse(null);return c!=null&&j!=null&&c.supportMode.equals("AI")&&c.aiGeneration==w.generation&&j.status.equals("RUNNING")&&w.token.equals(j.leaseToken)&&settings.ready();}));}
 private boolean valid(ChatConversation c,AiChatJob j,Work w){return c.supportMode.equals("AI")&&c.aiGeneration==w.generation&&j.status.equals("RUNNING")&&w.token.equals(j.leaseToken)&&settings.ready();}
 private void complete(Work w,AiAdvisor.Reply reply){
  var c=conversations.lockById(w.conversationId).orElse(null);var j=jobs.findById(w.id).orElse(null);if(c==null||j==null)return;
  if(!valid(c,j,w)){if(w.token.equals(j.leaseToken))j.status="SUPERSEDED";return;}
  if(!messages.existsByInReplyToId(w.sourceId)){
   var m=new ChatMessage();m.conversationId=c.id;m.senderRole="AI";m.senderName="Trợ lý AI TechZone";m.content=reply.answer();m.clientMessageId=UUID.randomUUID().toString();m.inReplyToId=w.sourceId;m.createdAt=clock.instant();m.productsJson=json.writeValueAsString(reply.products());messages.saveAndFlush(m);c.lastMessage=m.content;c.updatedAt=m.createdAt;
  }
  j.status="SUCCEEDED";j.inputTokens=reply.inputTokens();j.outputTokens=reply.outputTokens();c.aiCurrentJob=null;c.aiLeaseUntil=null;
  c.aiStatus=jobs.pending(c.id)>0?"QUEUED":"IDLE";
  if(reply.handoff())lifecycle.handoff(c);
 }
 private void fail(Work w,String code,boolean retry){
  var c=conversations.lockById(w.conversationId).orElse(null);var j=jobs.findById(w.id).orElse(null);if(c==null||j==null||!w.token.equals(j.leaseToken))return;
  if(!valid(c,j,w)){j.status="SUPERSEDED";return;}
  j.errorCode=code;c.aiLeaseUntil=null;c.aiCurrentJob=null;
  if(retry&&j.attempts<2&&!code.equals("SUPERSEDED")){j.status="QUEUED";j.nextAttemptAt=clock.instant().plusSeconds(2+ThreadLocalRandom.current().nextInt(3));c.aiStatus="QUEUED";}
  else{j.status="FAILED";lifecycle.handoff(c);c.aiStatus="FAILED";}
 }
}
