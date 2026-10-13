ALTER TABLE users ADD COLUMN default_address_id BIGINT NULL;
CREATE TABLE customer_addresses (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL, recipient_name VARCHAR(100) NOT NULL,
 phone VARCHAR(10) NOT NULL, address_text VARCHAR(500) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_address_owner FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
 INDEX idx_address_owner(user_id,id)
);
