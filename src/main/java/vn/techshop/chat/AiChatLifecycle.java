package vn.techshop.chat;

import java.time.*;
import org.springframework.stereotype.Service;
import vn.techshop.ai.*;

/** Called within the caller's conversation transaction. */
@Service
class AiChatLifecycle {
 private final AiJobRepository jobs;private final AiUsage usage;private final AiSettings settings;private final Clock clock;
 AiChatLifecycle(AiJobRepository jobs,AiUsage usage,AiSettings settings,Clock clock){this.jobs=jobs;this.usage=usage;this.settings=settings;this.clock=clock;}
 void enqueue(ChatConversation c,ChatMessage m){
  if(!c.supportMode.equals("AI")){if(c.handoffStatus.equals("CLOSED"))c.handoffStatus="WAITING";return;}
  if(!settings.ready()||AiAdvisor.privateRequest(m.content)){handoff(c);return;}
  // A quota failure retains the customer's human-chat message and routes it to staff.
  if(!usage.tryTurn(c.customerId)){handoff(c);return;}
  var j=new AiChatJob();j.conversationId=c.id;j.sourceMessageId=m.id;j.generation=c.aiGeneration;j.status="QUEUED";j.createdAt=clock.instant();j.nextAttemptAt=j.createdAt;
  jobs.save(j);c.aiStatus="QUEUED";
 }
 void handoff(ChatConversation c){c.supportMode="ADMIN";c.handoffStatus="WAITING";c.handoffAt=clock.instant();invalidate(c);}
 void accept(ChatConversation c,Long admin){c.supportMode="ADMIN";c.handoffStatus="ACCEPTED";c.acceptedAdminId=admin;invalidate(c);}
 void invalidate(ChatConversation c){c.aiGeneration++;c.aiStatus="IDLE";c.aiLeaseUntil=null;c.aiCurrentJob=null;jobs.supersede(c.id);}
}
