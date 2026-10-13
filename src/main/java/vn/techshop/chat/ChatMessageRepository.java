package vn.techshop.chat;

import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

interface ChatMessageRepository extends JpaRepository<ChatMessage,Long> {
    List<ChatMessage> findByConversationIdOrderByIdDesc(Long conversationId,Pageable page);
    List<ChatMessage> findByConversationIdAndIdGreaterThanOrderByIdAsc(Long conversationId,Long afterId,Pageable page);
    List<ChatMessage> findByConversationIdAndIdLessThanOrderByIdDesc(Long conversationId,Long beforeId,Pageable page);
    Optional<ChatMessage> findByConversationIdAndSenderIdAndClientMessageId(Long conversationId,Long senderId,String clientId);
    boolean existsByInReplyToId(Long id);
    @Query("select m from ChatMessage m where m.conversationId=:id and m.id>:start and m.id<=:end and m.senderRole in ('CUSTOMER','AI') order by m.id desc")
    List<ChatMessage> context(@Param("id") Long id,@Param("start") long start,@Param("end") long end,Pageable page);
    @Query("select coalesce(max(m.id),0) from ChatMessage m where m.conversationId=:id")
    long latestId(@Param("id") Long id);
    boolean existsByIdAndConversationId(Long id,Long conversationId);
    @Query("select count(m) from ChatMessage m join ChatConversation c on c.id=m.conversationId where c.customerId=:customerId and m.senderRole in ('ADMIN','AI') and m.id>c.customerReadId")
    long customerUnread(@Param("customerId") Long customerId);
    @Query("select count(m) from ChatMessage m join ChatConversation c on c.id=m.conversationId where m.senderRole='CUSTOMER' and m.id>c.adminReadId and c.supportMode='ADMIN'")
    long adminUnread();
}
