package vn.techshop.chat;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="chat_messages")
class ChatMessage {
    @Column(name="in_reply_to_message_id") Long inReplyToId;
    @Column(name="products_json",columnDefinition="TEXT") String productsJson;
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="conversation_id",nullable=false) Long conversationId;
    @Column(name="sender_id") Long senderId;
    @Column(name="sender_role",nullable=false,length=16) String senderRole;
    @Column(name="sender_name",nullable=false,length=100) String senderName;
    @Column(nullable=false,length=2000) String content;
    @Column(name="client_message_id",nullable=false,length=36) String clientMessageId;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="created_at",nullable=false) Instant createdAt;
}
