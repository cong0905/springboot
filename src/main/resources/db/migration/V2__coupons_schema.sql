-- Flyway migration V2: Coupons schema and order discount support
CREATE TABLE coupons (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(50) NOT NULL,
  description VARCHAR(255) NULL,
  discount_type VARCHAR(16) NOT NULL,
  discount_value DECIMAL(15,0) NOT NULL,
  min_order_amount DECIMAL(15,0) NOT NULL DEFAULT 0,
  max_discount_amount DECIMAL(15,0) NULL,
  max_uses INT NOT NULL DEFAULT 100,
  used_count INT NOT NULL DEFAULT 0,
  expires_at DATETIME(6) NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_coupons_code UNIQUE (code),
  CONSTRAINT ck_coupons_type CHECK (discount_type IN ('PERCENT', 'FIXED')),
  CONSTRAINT ck_coupons_value CHECK (discount_value > 0),
  CONSTRAINT ck_coupons_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
  CONSTRAINT ck_coupons_version CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE orders ADD COLUMN coupon_code VARCHAR(50) NULL AFTER note;
ALTER TABLE orders ADD COLUMN discount_amount DECIMAL(15,0) NOT NULL DEFAULT 0 AFTER shipping_fee;
ALTER TABLE orders ADD CONSTRAINT ck_orders_discount CHECK (discount_amount >= 0);
