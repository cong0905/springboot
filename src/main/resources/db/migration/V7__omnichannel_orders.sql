ALTER TABLE orders ADD COLUMN fulfillment_type VARCHAR(32) NOT NULL DEFAULT 'HOME_DELIVERY_STANDARD';
ALTER TABLE orders ADD COLUMN pickup_branch_id BIGINT NULL;
ALTER TABLE orders ADD COLUMN assigned_branch_id BIGINT NULL;
ALTER TABLE orders ADD COLUMN pickup_time DATETIME(6) NULL;
ALTER TABLE orders ADD COLUMN hold_expires_at DATETIME(6) NULL;
ALTER TABLE orders ADD COLUMN pickup_code VARCHAR(36) NULL;
ALTER TABLE orders ADD CONSTRAINT fk_pickup_branch FOREIGN KEY(pickup_branch_id) REFERENCES store_branches(id);
ALTER TABLE orders ADD CONSTRAINT fk_assigned_branch FOREIGN KEY(assigned_branch_id) REFERENCES store_branches(id);
CREATE INDEX ix_pickup_expiry ON orders(fulfillment_type,status,hold_expires_at);

ALTER TABLE orders DROP CHECK ck_orders_amounts;
ALTER TABLE orders ADD CONSTRAINT ck_orders_amounts_v7 CHECK(subtotal>=0 AND shipping_fee>=0 AND discount_amount BETWEEN 0 AND subtotal AND total_amount=subtotal-discount_amount+shipping_fee);
ALTER TABLE orders DROP CHECK ck_orders_payment_status;
ALTER TABLE orders ADD CONSTRAINT ck_orders_payment_status_v7 CHECK(payment_status IN ('UNPAID','PENDING','PAID','REFUNDED'));
