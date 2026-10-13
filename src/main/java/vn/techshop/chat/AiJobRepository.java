package vn.techshop.chat;

import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
interface AiJobRepository extends JpaRepository<AiChatJob,Long> {
 @Query("select j from AiChatJob j where (j.status='QUEUED' and j.nextAttemptAt<=:now) or (j.status='RUNNING' and j.leaseUntil<:now) order by j.id")
 List<AiChatJob> eligible(@Param("now") Instant now,Pageable page);
 @Query("select count(j) from AiChatJob j where j.conversationId=:id and j.status in ('QUEUED','RUNNING')")
 long pending(@Param("id") Long id);
 @Query("select count(j) from AiChatJob j where j.conversationId=:id and j.id<:job and j.status in ('QUEUED','RUNNING')")
 long earlier(@Param("id") Long id,@Param("job") Long job);
 @Modifying @Query("update AiChatJob j set j.status='SUPERSEDED' where j.conversationId=:id and j.status in ('QUEUED','RUNNING')")
 void supersede(@Param("id") Long id);
}
