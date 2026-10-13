ALTER TABLE chat_messages DROP CONSTRAINT chk_chat_sender_role;
ALTER TABLE chat_messages ALTER COLUMN sender_id DROP NOT NULL;
ALTER TABLE chat_conversations ADD COLUMN support_mode VARCHAR(16) NOT NULL DEFAULT 'ADMIN';
ALTER TABLE chat_conversations ADD COLUMN handoff_status VARCHAR(16) NOT NULL DEFAULT 'NONE';
ALTER TABLE chat_conversations ADD COLUMN ai_status VARCHAR(16) NOT NULL DEFAULT 'IDLE';
ALTER TABLE chat_conversations ADD COLUMN ai_generation BIGINT NOT NULL DEFAULT 0;
ALTER TABLE chat_conversations ADD COLUMN ai_start_message_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE chat_conversations ADD COLUMN accepted_admin_id BIGINT NULL;
ALTER TABLE chat_conversations ADD COLUMN handoff_at DATETIME(6) NULL;
ALTER TABLE chat_conversations ADD COLUMN ai_lease_until DATETIME(6) NULL;
ALTER TABLE chat_conversations ADD COLUMN ai_current_job BIGINT NULL;
ALTER TABLE chat_messages ADD COLUMN in_reply_to_message_id BIGINT NULL;
ALTER TABLE chat_messages ADD COLUMN products_json TEXT NULL;
ALTER TABLE chat_messages ADD CONSTRAINT uq_chat_ai_reply UNIQUE (in_reply_to_message_id);
ALTER TABLE chat_messages ADD CONSTRAINT fk_chat_ai_reply FOREIGN KEY (in_reply_to_message_id) REFERENCES chat_messages(id) ON DELETE CASCADE;
ALTER TABLE chat_messages ADD CONSTRAINT chk_chat_sender_kind CHECK ((sender_role='AI' AND sender_id IS NULL) OR (sender_role IN ('CUSTOMER','ADMIN') AND sender_id IS NOT NULL));
CREATE TABLE ai_chat_jobs (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 conversation_id BIGINT NOT NULL,
 source_message_id BIGINT NOT NULL UNIQUE,
 captured_generation BIGINT NOT NULL,
 status VARCHAR(24) NOT NULL,
 attempt_count INT NOT NULL DEFAULT 0,
 lease_token VARCHAR(36) NULL,
 lease_until DATETIME(6) NULL,
 deadline_at DATETIME(6) NULL,
 next_attempt_at DATETIME(6) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 error_code VARCHAR(64) NULL,
 model_name VARCHAR(100) NULL,
 input_tokens INT NOT NULL DEFAULT 0,
 output_tokens INT NOT NULL DEFAULT 0,
 CONSTRAINT fk_ai_job_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id) ON DELETE CASCADE,
 CONSTRAINT fk_ai_job_source FOREIGN KEY (source_message_id) REFERENCES chat_messages(id) ON DELETE CASCADE
);
CREATE INDEX idx_ai_jobs_ready ON ai_chat_jobs(status,next_attempt_at,id);
CREATE TABLE ai_usage_gate (id INT PRIMARY KEY);
INSERT INTO ai_usage_gate(id) VALUES(1);
CREATE TABLE ai_usage_limits (usage_key VARCHAR(128) PRIMARY KEY,usage_count INT NOT NULL,created_at DATETIME(6) NOT NULL);
