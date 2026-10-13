package vn.techshop.chat;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="ai_chat_jobs")
class AiChatJob {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Column(name="conversation_id",nullable=false) Long conversationId;
 @Column(name="source_message_id",nullable=false,unique=true) Long sourceMessageId;
 @Column(name="captured_generation",nullable=false) long generation;
 @Column(nullable=false,length=24) String status;
 @Column(name="attempt_count",nullable=false) int attempts;
 @Column(name="lease_token",length=36) String leaseToken;
 @JdbcTypeCode(SqlTypes.TIMESTAMP) @Column(name="lease_until") Instant leaseUntil;
 @JdbcTypeCode(SqlTypes.TIMESTAMP) @Column(name="next_attempt_at",nullable=false) Instant nextAttemptAt;
 @JdbcTypeCode(SqlTypes.TIMESTAMP) @Column(name="created_at",nullable=false) Instant createdAt;
 @JdbcTypeCode(SqlTypes.TIMESTAMP) @Column(name="deadline_at") Instant deadlineAt;
 @Column(name="error_code",length=64) String errorCode;
 @Column(name="model_name",length=100) String model;
 @Column(name="input_tokens",nullable=false) int inputTokens;
 @Column(name="output_tokens",nullable=false) int outputTokens;
}
