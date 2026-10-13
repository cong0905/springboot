package vn.techshop.chat;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

interface ChatConversationRepository extends JpaRepository<ChatConversation,Long> {
    Optional<ChatConversation> findByCustomerId(Long customerId);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ChatConversation c where c.id=:id")
    Optional<ChatConversation> lockById(@Param("id") Long id);
    @Query("select c.id from ChatConversation c where c.supportMode='AI' order by c.id")
    java.util.List<Long> aiConversations(Pageable pageable);
    interface Summary {
        Long getId(); Long getCustomerId(); String getCustomerName(); String getCustomerEmail();
        String getSupportMode(); String getHandoffStatus(); String getAiStatus(); String getLastMessage(); Instant getUpdatedAt(); long getUnreadCount();
    }
    @Query(value="""
        select c.id as id, c.customerId as customerId, u.fullName as customerName,
        u.email as customerEmail, c.supportMode as supportMode, c.handoffStatus as handoffStatus, c.aiStatus as aiStatus, c.lastMessage as lastMessage, c.updatedAt as updatedAt,
        (select count(m) from ChatMessage m where m.conversationId=c.id
            and m.senderRole='CUSTOMER' and m.id>c.adminReadId and c.supportMode='ADMIN') as unreadCount
        from ChatConversation c join AppUser u on u.id=c.customerId
        order by c.updatedAt desc, c.id desc
        """,countQuery="select count(c) from ChatConversation c")
    Page<Summary> inbox(Pageable pageable);
}
