CREATE TABLE password_reset_tokens (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL,
 token_hash CHAR(64) NOT NULL UNIQUE, expires_at TIMESTAMP(6) NOT NULL,
 used_at TIMESTAMP(6) NULL, delivery_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 CONSTRAINT fk_reset_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_reset_owner ON password_reset_tokens(user_id,expires_at);
CREATE TABLE google_identities (
 subject_id VARCHAR(255) PRIMARY KEY, user_id BIGINT NOT NULL UNIQUE,
 CONSTRAINT fk_google_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);
