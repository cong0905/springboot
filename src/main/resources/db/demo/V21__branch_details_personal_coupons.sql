ALTER TABLE store_branches ADD COLUMN opening_hours VARCHAR(300) NULL;
ALTER TABLE store_branches ADD COLUMN photo_path VARCHAR(500) NULL;
ALTER TABLE store_branches ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE coupons ADD COLUMN assigned_user_id BIGINT NULL;
ALTER TABLE coupons ADD CONSTRAINT fk_coupon_assignee FOREIGN KEY (assigned_user_id) REFERENCES users(id);
CREATE INDEX ix_coupon_assignee ON coupons(assigned_user_id);
