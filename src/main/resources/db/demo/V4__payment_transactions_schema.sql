-- H2 demo/test Flyway migration V4: Payment transactions schema
CREATE TABLE payment_transactions (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  transaction_code VARCHAR(100) NOT NULL,
  payment_gateway VARCHAR(32) NOT NULL,
  amount DECIMAL(15,0) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  bank_code VARCHAR(50) NULL,
  response_message VARCHAR(500) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT uq_payments_code UNIQUE (transaction_code),
  CONSTRAINT ck_payments_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
  CONSTRAINT ck_payments_version CHECK (version >= 0)
);

CREATE INDEX ix_payments_order ON payment_transactions (order_id, status);

ALTER TABLE orders ADD COLUMN payment_method VARCHAR(32) NOT NULL DEFAULT 'COD';
ALTER TABLE orders ADD COLUMN payment_status VARCHAR(16) NOT NULL DEFAULT 'UNPAID';
ALTER TABLE orders ADD CONSTRAINT ck_orders_payment_status CHECK (payment_status IN ('UNPAID', 'PAID', 'REFUNDED'));
