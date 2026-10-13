ALTER TABLE users ADD COLUMN guest_account BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE guest_order_proofs (
 token_hash CHAR(64) PRIMARY KEY, order_id BIGINT NOT NULL, expires_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT fk_guest_proof_order FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE
);
CREATE INDEX idx_guest_proof_order ON guest_order_proofs(order_id,expires_at);
