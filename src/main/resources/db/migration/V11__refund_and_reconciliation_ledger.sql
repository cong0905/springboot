CREATE TABLE refund_reconciliations (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  refund_code VARCHAR(64) NOT NULL,
  order_id BIGINT NOT NULL,
  payment_transaction_id BIGINT NULL,
  amount DECIMAL(15,0) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
  refund_type VARCHAR(32) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  actor_id BIGINT NOT NULL,
  processed_by BIGINT NULL,
  reference_code VARCHAR(100) NULL,
  gateway VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
  restock_items BOOLEAN NOT NULL DEFAULT FALSE,
  notes VARCHAR(1000) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_refunds_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  CONSTRAINT uq_refunds_code UNIQUE (refund_code),
  CONSTRAINT ck_refunds_status CHECK (status IN ('REQUESTED', 'PROCESSING', 'SUCCEEDED', 'FAILED')),
  CONSTRAINT ck_refunds_amount CHECK (amount > 0)
);

CREATE INDEX ix_refunds_order ON refund_reconciliations (order_id, status);
