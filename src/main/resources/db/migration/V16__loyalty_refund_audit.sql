CREATE TABLE point_reversals (
 order_id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL,
 points_delta BIGINT NOT NULL, spending_delta DECIMAL(15,0) NOT NULL,
 occurred_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT fk_point_reversal_order FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE
);
