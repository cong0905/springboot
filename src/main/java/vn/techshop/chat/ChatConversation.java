package vn.techshop.chat;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="chat_conversations")
class ChatConversation {
    @Column(name="support_mode",nullable=false,length=16) String supportMode="ADMIN";
    @Column(name="handoff_status",nullable=false,length=16) String handoffStatus="NONE";
    @Column(name="ai_status",nullable=false,length=16) String aiStatus="IDLE";
    @Column(name="ai_generation",nullable=false) long aiGeneration;
    @Column(name="ai_start_message_id",nullable=false) long aiStartMessageId;
    @Column(name="accepted_admin_id") Long acceptedAdminId;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) @Column(name="handoff_at") Instant handoffAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP) @Column(name="ai_lease_until") Instant aiLeaseUntil;
    @Column(name="ai_current_job") Long aiCurrentJob;
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="customer_id",nullable=false,unique=true) Long customerId;
    @Column(name="customer_read_id",nullable=false) long customerReadId;
    @Column(name="admin_read_id",nullable=false) long adminReadId;
    @Column(name="last_message",nullable=false,length=2000) String lastMessage;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="updated_at",nullable=false) Instant updatedAt;
}
