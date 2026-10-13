CREATE TABLE chat_conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL UNIQUE,
    customer_read_id BIGINT NOT NULL DEFAULT 0,
    admin_read_id BIGINT NOT NULL DEFAULT 0,
    last_message VARCHAR(2000) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_chat_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_chat_conversations_updated ON chat_conversations(updated_at, id);
CREATE TABLE chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    sender_role VARCHAR(16) NOT NULL,
    sender_name VARCHAR(100) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    client_message_id VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_chat_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_chat_sender_role CHECK (sender_role IN ('CUSTOMER','ADMIN')),
    CONSTRAINT uq_chat_client_message UNIQUE (conversation_id, sender_id, client_message_id)
);
CREATE INDEX idx_chat_messages_unread ON chat_messages(conversation_id, sender_role, id);
