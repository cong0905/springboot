-- ==============================================================================
-- TECHSHOP OMNICHANNEL COMMERCE - FULL SCHEMA & SAMPLE DATA (CELLPHONES MODEL)
-- Nền tảng: MySQL 8.0 / 8.4+ | InnoDB | UTF8MB4
-- Hướng dẫn thực thi trên MySQL Workbench:
-- 1. Mở MySQL Workbench, kết nối vào Local Instance (hoặc Database server của bạn).
-- 2. Vào menu File -> Open SQL Script... -> chọn file này.
-- 3. Bấm icon tia sét (Execute) hoặc nhấn tổ hợp phím Ctrl + Shift + Enter.
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS techshop CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE techshop;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. BẢNG TÀI KHOẢN & NGƯỜI DÙNG
DROP TABLE IF EXISTS repair_tickets;
DROP TABLE IF EXISTS warranty_cards;
DROP TABLE IF EXISTS point_transactions;
DROP TABLE IF EXISTS member_profiles;
DROP TABLE IF EXISTS installment_applications;
DROP TABLE IF EXISTS installment_plans;
DROP TABLE IF EXISTS trade_in_requests;
DROP TABLE IF EXISTS trade_in_criterias;
DROP TABLE IF EXISTS trade_in_devices;
DROP TABLE IF EXISTS payment_transactions;
DROP TABLE IF EXISTS product_reviews;
DROP TABLE IF EXISTS order_status_history;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS coupon_usages;
DROP TABLE IF EXISTS coupons;
DROP TABLE IF EXISTS cart_items;
DROP TABLE IF EXISTS carts;
DROP TABLE IF EXISTS imei_history;
DROP TABLE IF EXISTS product_items_imei;
DROP TABLE IF EXISTS branch_inventories;
DROP TABLE IF EXISTS store_branches;
DROP TABLE IF EXISTS stock_movements;
DROP TABLE IF EXISTS product_specifications;
DROP TABLE IF EXISTS product_images;
DROP TABLE IF EXISTS product_variants;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- ==============================================================================
-- DDL - TẠO CẤU TRÚC BẢNG (CHUẨN FLYWAY V1 -> V9)
-- ==============================================================================

CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(254) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(16) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_users_email UNIQUE (email),
  CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE categories (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(50) NOT NULL,
  name VARCHAR(100) NOT NULL,
  status VARCHAR(16) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_categories_code UNIQUE (code),
  CONSTRAINT ck_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
  CONSTRAINT ck_categories_version CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE products (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  category_id BIGINT NOT NULL,
  sku VARCHAR(50) NOT NULL,
  name VARCHAR(200) NOT NULL,
  description TEXT NULL,
  price DECIMAL(15,0) NOT NULL,
  stock_quantity INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id),
  CONSTRAINT uq_products_sku UNIQUE (sku),
  CONSTRAINT ck_products_price CHECK (price BETWEEN 0 AND 999999999),
  CONSTRAINT ck_products_stock CHECK (stock_quantity >= 0),
  CONSTRAINT ck_products_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
  CONSTRAINT ck_products_version CHECK (version >= 0),
  INDEX ix_products_catalog (status, category_id, price, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_variants (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  sku_code VARCHAR(50) NOT NULL UNIQUE,
  color_name VARCHAR(80) NOT NULL,
  storage_capacity VARCHAR(80) NOT NULL,
  price DECIMAL(15,0) NOT NULL,
  original_price DECIMAL(15,0) NOT NULL,
  status VARCHAR(16) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_variant_product FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE,
  CONSTRAINT ck_variant_price CHECK(price >= 0 AND original_price >= price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_images (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  variant_id BIGINT NULL,
  asset_path VARCHAR(255) NOT NULL,
  alt_text VARCHAR(200) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
  CONSTRAINT fk_image_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE,
  CONSTRAINT ck_images_sort CHECK (sort_order >= 0),
  INDEX ix_images_product (product_id, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_specifications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  spec_group VARCHAR(80) NOT NULL,
  spec_name VARCHAR(80) NOT NULL,
  spec_value VARCHAR(500) NOT NULL,
  display_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_spec_product FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE,
  UNIQUE(product_id, spec_group, spec_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE store_branches (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(50) NOT NULL UNIQUE,
  address VARCHAR(500) NOT NULL,
  province_code VARCHAR(20) NOT NULL,
  district_code VARCHAR(20),
  ward_code VARCHAR(20),
  phone VARCHAR(20) NOT NULL,
  latitude DOUBLE NOT NULL,
  longitude DOUBLE NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE branch_inventories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  branch_id BIGINT NOT NULL,
  variant_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 0,
  reserved_quantity INT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0,
  FOREIGN KEY(branch_id) REFERENCES store_branches(id),
  FOREIGN KEY(variant_id) REFERENCES product_variants(id) ON DELETE CASCADE,
  UNIQUE(branch_id, variant_id),
  CHECK(quantity >= 0 AND reserved_quantity >= 0 AND reserved_quantity <= quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_items_imei (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  variant_id BIGINT NOT NULL,
  branch_id BIGINT NOT NULL,
  imei_number VARCHAR(40) NOT NULL UNIQUE,
  status VARCHAR(16) NOT NULL,
  order_item_id BIGINT NULL,
  imported_at DATETIME(6) NOT NULL,
  sold_at DATETIME(6),
  FOREIGN KEY(variant_id) REFERENCES product_variants(id) ON DELETE CASCADE,
  FOREIGN KEY(branch_id) REFERENCES store_branches(id),
  CHECK(status IN ('IN_STOCK','RESERVED','SOLD','DEFECTIVE','RETURNED')),
  INDEX ix_imei_stock(variant_id, branch_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE imei_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  imei_id BIGINT NOT NULL,
  from_status VARCHAR(16),
  to_status VARCHAR(16) NOT NULL,
  from_branch_id BIGINT,
  to_branch_id BIGINT NOT NULL,
  reason VARCHAR(500) NOT NULL,
  occurred_at DATETIME(6) NOT NULL,
  FOREIGN KEY(imei_id) REFERENCES product_items_imei(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE stock_movements (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  quantity_delta INT NOT NULL,
  balance_after INT NOT NULL,
  reason VARCHAR(255) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_movements_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_movements_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT ck_movements_delta CHECK (quantity_delta <> 0),
  CONSTRAINT ck_movements_balance CHECK (balance_after >= 0),
  INDEX ix_movements_product (product_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE carts (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT uq_carts_user UNIQUE (user_id),
  CONSTRAINT ck_carts_version CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cart_items (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  cart_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  variant_id BIGINT NULL,
  quantity INT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id),
  CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_cart_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id),
  CONSTRAINT uq_cart_variant UNIQUE(cart_id, product_id, variant_id),
  CONSTRAINT ck_cart_items_qty CHECK (quantity BETWEEN 1 AND 99)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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

CREATE TABLE orders (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_code VARCHAR(32) NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL,
  subtotal DECIMAL(15,0) NOT NULL,
  shipping_fee DECIMAL(15,0) NOT NULL,
  discount_amount DECIMAL(15,0) NOT NULL DEFAULT 0,
  total_amount DECIMAL(15,0) NOT NULL,
  recipient_name VARCHAR(100) NOT NULL,
  recipient_phone VARCHAR(20) NOT NULL,
  shipping_address VARCHAR(255) NOT NULL,
  note VARCHAR(255) NULL,
  coupon_code VARCHAR(50) NULL,
  payment_method VARCHAR(32) NOT NULL DEFAULT 'COD',
  payment_status VARCHAR(16) NOT NULL DEFAULT 'UNPAID',
  fulfillment_type VARCHAR(32) NOT NULL DEFAULT 'HOME_DELIVERY_STANDARD',
  pickup_branch_id BIGINT NULL,
  assigned_branch_id BIGINT NULL,
  pickup_time DATETIME(6) NULL,
  hold_expires_at DATETIME(6) NULL,
  pickup_code VARCHAR(36) NULL,
  checkout_request_hash VARCHAR(64) NOT NULL,
  idempotency_key VARCHAR(64) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  delivered_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_pickup_branch FOREIGN KEY (pickup_branch_id) REFERENCES store_branches(id),
  CONSTRAINT fk_assigned_branch FOREIGN KEY (assigned_branch_id) REFERENCES store_branches(id),
  CONSTRAINT uq_orders_code UNIQUE (order_code),
  CONSTRAINT uq_orders_idempotency UNIQUE (user_id, idempotency_key),
  CONSTRAINT ck_orders_status CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
  CONSTRAINT ck_orders_payment_status CHECK (payment_status IN ('UNPAID', 'PENDING', 'PAID', 'REFUNDED')),
  CONSTRAINT ck_orders_amounts CHECK (subtotal >= 0 AND shipping_fee >= 0 AND discount_amount BETWEEN 0 AND subtotal AND total_amount = subtotal - discount_amount + shipping_fee),
  CONSTRAINT ck_orders_version CHECK (version >= 0),
  INDEX ix_orders_lookup (status, created_at, id),
  INDEX ix_orders_delivered (delivered_at, status),
  INDEX ix_pickup_expiry (fulfillment_type, status, hold_expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_items (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  variant_id BIGINT NULL,
  name VARCHAR(200) NOT NULL,
  unit_price DECIMAL(15,0) NOT NULL,
  quantity INT NOT NULL,
  line_total DECIMAL(15,0) NOT NULL,
  CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT uq_order_variant UNIQUE(order_id, product_id, variant_id),
  CONSTRAINT ck_order_items_qty CHECK (quantity BETWEEN 1 AND 99),
  CONSTRAINT ck_order_items_price CHECK (unit_price BETWEEN 0 AND 999999999),
  CONSTRAINT ck_order_items_total CHECK (line_total = unit_price * quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_status_history (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  from_status VARCHAR(16) NULL,
  to_status VARCHAR(16) NOT NULL,
  actor_user_id BIGINT NOT NULL,
  note VARCHAR(255) NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_order_history_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_order_history_user FOREIGN KEY (actor_user_id) REFERENCES users(id),
  CONSTRAINT ck_history_to_status CHECK (to_status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
  INDEX ix_history_order (order_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE product_reviews (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  author_name VARCHAR(100) NOT NULL,
  rating INT NOT NULL,
  comment TEXT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'APPROVED',
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5),
  CONSTRAINT ck_reviews_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
  CONSTRAINT ck_reviews_version CHECK (version >= 0),
  INDEX ix_reviews_product (product_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
  CONSTRAINT ck_payments_version CHECK (version >= 0),
  INDEX ix_payments_order (order_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE trade_in_devices (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  model VARCHAR(100) NOT NULL UNIQUE,
  base_price DECIMAL(15,0) NOT NULL,
  subsidy_amount DECIMAL(15,0) NOT NULL DEFAULT 0,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  CHECK(base_price >= 0 AND subsidy_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE trade_in_criterias (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  deduction_percent INT NOT NULL,
  CHECK(deduction_percent BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE trade_in_requests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  device_id BIGINT NOT NULL,
  criteria_id BIGINT NOT NULL,
  old_imei VARCHAR(40) NOT NULL UNIQUE,
  quoted_amount DECIMAL(15,0) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'QUOTED',
  order_id BIGINT NULL UNIQUE,
  created_at DATETIME(6) NOT NULL,
  FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY(device_id) REFERENCES trade_in_devices(id),
  FOREIGN KEY(criteria_id) REFERENCES trade_in_criterias(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE installment_plans (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  provider VARCHAR(100) NOT NULL,
  months INT NOT NULL,
  fee_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  CHECK(months IN(3,6,9,12) AND fee_percent >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE installment_applications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL UNIQUE,
  plan_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  financed_amount DECIMAL(15,0) NOT NULL,
  FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE,
  FOREIGN KEY(plan_id) REFERENCES installment_plans(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE member_profiles (
  user_id BIGINT PRIMARY KEY,
  member_rank VARCHAR(16) NOT NULL DEFAULT 'S_NEW',
  accumulated_spending DECIMAL(15,0) NOT NULL DEFAULT 0,
  points_balance BIGINT NOT NULL DEFAULT 0,
  FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
  CHECK(points_balance >= 0 AND accumulated_spending >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE point_transactions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL UNIQUE,
  points_delta BIGINT NOT NULL,
  occurred_at DATETIME(6) NOT NULL,
  FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE warranty_cards (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  imei_number VARCHAR(40) NOT NULL UNIQUE,
  order_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  warranty_type VARCHAR(80) NOT NULL,
  FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE,
  FOREIGN KEY(customer_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE repair_tickets (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  warranty_id BIGINT NOT NULL,
  issue VARCHAR(500) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'RECEIVED',
  created_at DATETIME(6) NOT NULL,
  FOREIGN KEY(warranty_id) REFERENCES warranty_cards(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- ==============================================================================
-- DML - DỮ LIỆU MẪU ĐẶC TRƯNG CHUỖI BÁN LẺ CÔNG NGHỆ (CELLPHONES MODEL)
-- ==============================================================================

-- 1. Tài khoản mẫu
-- Admin: admin@techshop.test / DemoAdmin2026!
-- Khách hàng: customer@techshop.test / DemoCustomer2026!
INSERT INTO users (id, full_name, email, password_hash, role, created_at) VALUES
(1, 'Ban Quản Trị TechShop', 'admin@techshop.test', '$2a$10$rTpn9QzOfc4g6SYAH8OC4e3R1tcLEKWQLstX4.J4Y0wEoJSmKYtMS', 'ADMIN', '2026-01-01 00:00:00.000000'),
(2, 'Nguyễn Văn An (S-VIP)', 'customer@techshop.test', '$2a$10$vDLiKViROL3cbN65TKnW3.erYg.YIyCAK1CiYHHoo8.yHbvtPy/Ba', 'CUSTOMER', '2026-01-15 08:30:00.000000'),
(3, 'Trần Hoàng Long (S-MEM)', 'hoanglong@techshop.test', '$2a$10$vDLiKViROL3cbN65TKnW3.erYg.YIyCAK1CiYHHoo8.yHbvtPy/Ba', 'CUSTOMER', '2026-02-10 09:15:00.000000'),
(4, 'Lê Linh Đan (S-NEW)', 'linhdan@techshop.test', '$2a$10$vDLiKViROL3cbN65TKnW3.erYg.YIyCAK1CiYHHoo8.yHbvtPy/Ba', 'CUSTOMER', '2026-03-01 10:00:00.000000');

-- 2. Hồ sơ hội viên Smember & Điểm thưởng S-Point
INSERT INTO member_profiles (user_id, member_rank, accumulated_spending, points_balance) VALUES
(2, 'S_VIP', 45000000, 450),
(3, 'S_MEM', 18500000, 185),
(4, 'S_NEW', 0, 0);

-- 3. Danh mục sản phẩm công nghệ
INSERT INTO categories (id, code, name, status, version, created_at, updated_at) VALUES
(1, 'PHONE', 'Điện thoại Smartphone', 'ACTIVE', 0, '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000'),
(2, 'LAPTOP', 'Laptop & MacBook', 'ACTIVE', 0, '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000'),
(3, 'TABLET', 'Máy tính bảng iPad & Android', 'ACTIVE', 0, '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000'),
(4, 'AUDIO', 'Tai nghe & Loa Bluetooth', 'ACTIVE', 0, '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000'),
(5, 'WATCH', 'Đồng hồ thông minh Smartwatch', 'ACTIVE', 0, '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000'),
(6, 'GEAR', 'Phụ kiện Củ sạc & Cáp kết nối', 'ACTIVE', 0, '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000');

-- 4. Chuỗi chi nhánh cửa hàng (O2O Omnichannel)
INSERT INTO store_branches (id, name, code, address, province_code, district_code, ward_code, phone, latitude, longitude, is_active) VALUES
(1, 'TechShop Thái Hà', 'HN-TH', '123 Thái Hà, P. Trung Liệt, Q. Đống Đa, Hà Nội', 'HN', 'DD', 'TL', '02473000123', 21.0126, 105.8197, TRUE),
(2, 'TechShop Cầu Giấy', 'HN-CG', '100 Cầu Giấy, P. Quan Hoa, Q. Cầu Giấy, Hà Nội', 'HN', 'CG', 'QH', '02473000100', 21.0365, 105.7908, TRUE),
(3, 'TechShop Trần Quang Khải', 'HCM-TQK', '213 Trần Quang Khải, P. Tân Định, Quận 1, TP. Hồ Chí Minh', 'HCM', 'Q1', 'TD', '02873000213', 10.7915, 106.6912, TRUE),
(4, 'TechShop Nguyễn Thị Thập', 'HCM-NTT', '350 Nguyễn Thị Thập, P. Tân Quy, Quận 7, TP. Hồ Chí Minh', 'HCM', 'Q7', 'TQ', '02873000350', 10.7412, 106.7089, TRUE);

-- 5. Sản phẩm chính (Products)
INSERT INTO products (id, category_id, sku, name, description, price, stock_quantity, status, version, created_at, updated_at) VALUES
(1, 1, 'PHONE-IP16PM', 'iPhone 16 Pro Max 256GB - Chính hãng VN/A', 'Màn hình 6.9 inch Super Retina XDR OLED, Chip A18 Pro, Camera Fusion 48MP zoom quang 5x, khung viền Titanium siêu bền nhẹ.', 34490000, 20, 'ACTIVE', 0, '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000'),
(2, 1, 'PHONE-S25U', 'Samsung Galaxy S25 Ultra 5G 256GB', 'Chip Snapdragon 8 Elite mạnh mẽ nhất, bút S-Pen tích hợp, camera 200MP AI thế hệ mới, màn hình Dynamic AMOLED 2X 6.8 inch phẳng.', 31990000, 16, 'ACTIVE', 0, '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000'),
(3, 2, 'LAPTOP-MBP14M4', 'MacBook Pro 14 M4 Pro (24GB / 512GB SSD)', 'Hiệu năng đồ họa đỉnh cao với chip Apple M4 Pro 12-core, RAM 24GB Unified, màn hình Liquid Retina XDR 120Hz ProMotion tuyệt đẹp.', 49990000, 8, 'ACTIVE', 0, '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000'),
(4, 3, 'TABLET-IPADM4-11', 'iPad Pro 11 inch M4 Ultra Retina XDR (256GB)', 'Đột phá với màn hình Tandem OLED mỏng nhẹ nhất lịch sử Apple, chip M4 bứt phá sức mạnh sáng tạo và đồ họa chuyên nghiệp.', 27990000, 12, 'ACTIVE', 0, '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000'),
(5, 5, 'WATCH-AWS10-46', 'Apple Watch Series 10 Nhôm 46mm GPS', 'Thiết kế mỏng hơn 10%, màn hình góc nhìn rộng sáng rõ 2000 nits, cảm biến đo độ sâu và nhiệt độ nước, sạc nhanh 80% trong 30 phút.', 11290000, 15, 'ACTIVE', 0, '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000'),
(6, 4, 'AUDIO-SONY-XM5', 'Tai nghe chụp tai Sony WH-1000XM5 Hi-Res Audio', 'Khả năng chống ồn chủ động ANC hàng đầu thế giới với 2 bộ xử lý và 8 micro, thời lượng pin 30 giờ, công nghệ âm thanh Hi-Res LDAC.', 6990000, 14, 'ACTIVE', 0, '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000'),
(7, 4, 'AUDIO-MARSHALL-E2', 'Loa Bluetooth di động Marshall Emberton II', 'Âm thanh đa hướng 360 độ True Stereophonic đặc trưng Marshall, chuẩn kháng nước bụi IP67, pin bền bỉ hơn 30 giờ chơi nhạc.', 3890000, 10, 'ACTIVE', 0, '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000'),
(8, 6, 'GEAR-ANKER-67W', 'Củ sạc nhanh Anker GaNPrime 67W 3 Cổng (A2668)', 'Công nghệ GaNPrime tối ưu nhiệt độ và kích thước, sạc đồng thời 2 cổng USB-C và 1 cổng USB-A cho Laptop, Tablet và Smartphone.', 890000, 30, 'ACTIVE', 0, '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000');

-- 6. Biến thể sản phẩm (Variants: Màu sắc & Dung lượng)
INSERT INTO product_variants (id, product_id, sku_code, color_name, storage_capacity, price, original_price, status, version) VALUES
-- iPhone 16 Pro Max
(1, 1, 'IP16PM-256-DESERT', 'Titan Sa Mạc', '256GB', 34490000, 35990000, 'ACTIVE', 0),
(2, 1, 'IP16PM-256-BLACK', 'Titan Đen', '256GB', 34490000, 35990000, 'ACTIVE', 0),
(3, 1, 'IP16PM-512-NATURAL', 'Titan Tự Nhiên', '512GB', 40490000, 41990000, 'ACTIVE', 0),
-- Samsung Galaxy S25 Ultra
(4, 2, 'S25U-256-TITAN', 'Titanium Xám', '256GB', 31990000, 33990000, 'ACTIVE', 0),
(5, 2, 'S25U-256-BLACK', 'Titanium Đen', '256GB', 31990000, 33990000, 'ACTIVE', 0),
(6, 2, 'S25U-512-TITAN', 'Titanium Xám', '512GB', 36990000, 38990000, 'ACTIVE', 0),
-- MacBook Pro 14 M4
(7, 3, 'MBP14M4-512-BLACK', 'Space Black', '512GB SSD', 49990000, 49990000, 'ACTIVE', 0),
(8, 3, 'MBP14M4-1TB-SILVER', 'Silver', '1TB SSD', 55990000, 56990000, 'ACTIVE', 0),
-- iPad Pro M4 11
(9, 4, 'IPADM4-256-BLACK', 'Space Black', '256GB', 27990000, 28990000, 'ACTIVE', 0),
(10, 4, 'IPADM4-256-SILVER', 'Silver', '256GB', 27990000, 28990000, 'ACTIVE', 0),
-- Apple Watch Series 10
(11, 5, 'AWS10-46-JETBLACK', 'Nhôm Đen Bóng', '46mm GPS', 11290000, 11990000, 'ACTIVE', 0),
(12, 5, 'AWS10-46-SILVER', 'Nhôm Bạc', '46mm GPS', 11290000, 11990000, 'ACTIVE', 0),
-- Sony WH-1000XM5
(13, 6, 'SONY-XM5-BLACK', 'Đen Nhám', 'Tiêu chuẩn', 6990000, 7990000, 'ACTIVE', 0),
(14, 6, 'SONY-XM5-SILVER', 'Bạc Bạch Kim', 'Tiêu chuẩn', 6990000, 7990000, 'ACTIVE', 0),
-- Marshall Emberton II
(15, 7, 'MARSHALL-E2-BLACK', 'Black & Brass', 'Tiêu chuẩn', 3890000, 4290000, 'ACTIVE', 0),
-- Anker GaNPrime 67W
(16, 8, 'ANKER-67W-BLACK', 'Đen', '3 Cổng', 890000, 1150000, 'ACTIVE', 0);

-- 7. Hình ảnh sản phẩm (Product Images & Variant Mapping)
INSERT INTO product_images (id, product_id, variant_id, asset_path, alt_text, sort_order) VALUES
(1, 1, 1, '/assets/products/phone-titan.svg', 'iPhone 16 Pro Max Titan Sa Mạc', 0),
(2, 1, 2, '/assets/products/phone-black.svg', 'iPhone 16 Pro Max Titan Đen', 1),
(3, 1, 3, '/assets/products/phone-silver.svg', 'iPhone 16 Pro Max Titan Tự Nhiên', 2),
(4, 2, 4, '/assets/products/phone-silver.svg', 'Samsung Galaxy S25 Ultra Titanium Xám', 0),
(5, 2, 5, '/assets/products/phone-black.svg', 'Samsung Galaxy S25 Ultra Titanium Đen', 1),
(6, 3, 7, '/assets/products/laptop.svg', 'MacBook Pro 14 M4 Space Black', 0),
(7, 3, 8, '/assets/products/laptop.svg', 'MacBook Pro 14 M4 Silver', 1),
(8, 4, 9, '/assets/products/tablet.svg', 'iPad Pro 11 M4 Space Black', 0),
(9, 4, 10, '/assets/products/tablet.svg', 'iPad Pro 11 M4 Silver', 1),
(10, 5, 11, '/assets/products/watch.svg', 'Apple Watch Series 10 Jet Black', 0),
(11, 5, 12, '/assets/products/watch.svg', 'Apple Watch Series 10 Silver', 1),
(12, 6, 13, '/assets/products/device.svg', 'Sony WH-1000XM5 Black', 0),
(13, 7, 15, '/assets/products/speaker.svg', 'Marshall Emberton II Black & Brass', 0),
(14, 8, 16, '/assets/products/charger.svg', 'Anker GaNPrime 67W GaN Charger', 0);

-- 8. Thông số kỹ thuật sản phẩm (Specifications Sheet)
INSERT INTO product_specifications (product_id, spec_group, spec_name, spec_value, display_order) VALUES
-- iPhone 16 Pro Max
(1, 'Màn hình', 'Kích thước & Công nghệ', '6.9 inch Super Retina XDR OLED, 120Hz ProMotion', 0),
(1, 'Hiệu năng', 'Vi xử lý (CPU)', 'Apple A18 Pro 6 nhân, GPU 6 nhân, Neural Engine 16 nhân', 1),
(1, 'Camera', 'Hệ thống Camera sau', 'Chính 48MP + Góc siêu rộng 48MP + Tele 12MP Zoom 5x', 2),
(1, 'Pin & Sạc', 'Dung lượng pin', '4.685 mAh, Sạc nhanh 50% trong 30 phút, MagSafe 25W', 3),
(1, 'Thiết kế', 'Chất liệu khung viền', 'Titanium Cấp độ 5, Mặt trước Ceramic Shield thế hệ mới', 4),
-- Samsung Galaxy S25 Ultra
(2, 'Màn hình', 'Kích thước & Tấm nền', '6.8 inch Dynamic AMOLED 2X, QHD+, 1-120Hz thích ứng', 0),
(2, 'Hiệu năng', 'Bộ vi xử lý', 'Qualcomm Snapdragon 8 Elite for Galaxy (3nm)', 1),
(2, 'Camera', 'Cảm biến chính', '200MP Siêu chi tiết + 50MP Zoom 5x + 50MP Góc rộng', 2),
(2, 'Tiện ích', 'Bút thông minh', 'S-Pen tích hợp trong thân máy với độ trễ 2.8ms', 3),
(2, 'Pin & Sạc', 'Thông số pin', '5.000 mAh, Sạc siêu nhanh 45W Type-C', 4),
-- MacBook Pro 14 M4
(3, 'Hiệu năng', 'Vi xử lý CPU/GPU', 'Apple M4 Pro (12-core CPU, 16-core GPU, 16-core NPU)', 0),
(3, 'Bộ nhớ', 'Bộ nhớ RAM & Ổ cứng', '24GB Unified Memory, SSD 512GB tốc độ cao', 1),
(3, 'Màn hình', 'Tấm nền hiển thị', '14.2 inch Liquid Retina XDR (3024 x 1964), 1600 nits', 2),
(3, 'Cổng kết nối', 'Giao tiếp ngoại vi', '3x Thunderbolt 5, HDMI, SDXC, MagSafe 3, Jack 3.5mm', 3),
-- iPad Pro M4 11
(4, 'Màn hình', 'Công nghệ hiển thị', '11 inch Tandem OLED Ultra Retina XDR, ProMotion 120Hz', 0),
(4, 'Hiệu năng', 'Chip xử lý', 'Apple M4 thế hệ mới, hỗ trợ Ray Tracing phần cứng', 1),
(4, 'Thiết kế', 'Độ mỏng & Khối lượng', 'Mỏng chỉ 5.3 mm, nhẹ 444 gram', 2);

-- 9. Tồn kho chi nhánh (Branch Inventory)
INSERT INTO branch_inventories (branch_id, variant_id, quantity, reserved_quantity, version) VALUES
-- TechShop Thái Hà (HN-TH)
(1, 1, 5, 0, 0),
(1, 2, 5, 0, 0),
(1, 3, 3, 0, 0),
(1, 4, 4, 0, 0),
(1, 5, 4, 0, 0),
(1, 7, 2, 0, 0),
(1, 9, 3, 0, 0),
(1, 11, 4, 0, 0),
(1, 13, 5, 0, 0),
(1, 15, 3, 0, 0),
(1, 16, 10, 0, 0),
-- TechShop Cầu Giấy (HN-CG)
(2, 1, 3, 0, 0),
(2, 2, 3, 0, 0),
(2, 4, 3, 0, 0),
(2, 7, 2, 0, 0),
(2, 11, 3, 0, 0),
(2, 16, 8, 0, 0),
-- TechShop Trần Quang Khải (HCM-TQK)
(3, 1, 5, 0, 0),
(3, 2, 4, 0, 0),
(3, 4, 4, 0, 0),
(3, 6, 2, 0, 0),
(3, 7, 2, 0, 0),
(3, 9, 3, 0, 0),
(3, 13, 4, 0, 0),
(3, 15, 3, 0, 0),
(3, 16, 6, 0, 0),
-- TechShop Nguyễn Thị Thập (HCM-NTT)
(4, 1, 3, 0, 0),
(4, 3, 2, 0, 0),
(4, 4, 3, 0, 0),
(4, 8, 2, 0, 0),
(4, 10, 3, 0, 0),
(4, 12, 3, 0, 0),
(4, 16, 6, 0, 0);

-- 10. Danh mục mã Serial / IMEI từng chiếc máy độc bản (Product Items IMEI)
INSERT INTO product_items_imei (variant_id, branch_id, imei_number, status, imported_at) VALUES
-- iPhone 16 Pro Max Titan Sa Mạc tại Thái Hà
(1, 1, '358920110001001', 'IN_STOCK', '2026-03-01 08:00:00.000000'),
(1, 1, '358920110001002', 'IN_STOCK', '2026-03-01 08:00:00.000000'),
(1, 1, '358920110001003', 'IN_STOCK', '2026-03-01 08:00:00.000000'),
(1, 1, '358920110001004', 'IN_STOCK', '2026-03-01 08:00:00.000000'),
(1, 1, '358920110001005', 'IN_STOCK', '2026-03-01 08:00:00.000000'),
-- iPhone 16 Pro Max Titan Đen tại Thái Hà
(2, 1, '358920110002001', 'IN_STOCK', '2026-03-01 08:00:00.000000'),
(2, 1, '358920110002002', 'IN_STOCK', '2026-03-01 08:00:00.000000'),
(2, 1, '358920110002003', 'IN_STOCK', '2026-03-01 08:00:00.000000'),
(2, 1, '358920110002004', 'IN_STOCK', '2026-03-01 08:00:00.000000'),
(2, 1, '358920110002005', 'IN_STOCK', '2026-03-01 08:00:00.000000'),
-- iPhone 16 Pro Max tại Trần Quang Khải (TP.HCM)
(1, 3, '358920110003001', 'IN_STOCK', '2026-03-02 08:00:00.000000'),
(1, 3, '358920110003002', 'IN_STOCK', '2026-03-02 08:00:00.000000'),
(1, 3, '358920110003003', 'IN_STOCK', '2026-03-02 08:00:00.000000'),
(1, 3, '358920110003004', 'IN_STOCK', '2026-03-02 08:00:00.000000'),
(1, 3, '358920110003005', 'IN_STOCK', '2026-03-02 08:00:00.000000'),
-- Galaxy S25 Ultra tại Thái Hà
(4, 1, '357419120001001', 'IN_STOCK', '2026-03-01 08:00:00.000000'),
(4, 1, '357419120001002', 'IN_STOCK', '2026-03-01 08:00:00.000000'),
(4, 1, '357419120001003', 'IN_STOCK', '2026-03-01 08:00:00.000000'),
(4, 1, '357419120001004', 'IN_STOCK', '2026-03-01 08:00:00.000000'),
-- MacBook Pro 14 M4 tại Thái Hà & TQK
(7, 1, 'C02G1001M4P1', 'IN_STOCK', '2026-03-01 08:00:00.000000'),
(7, 1, 'C02G1001M4P2', 'IN_STOCK', '2026-03-01 08:00:00.000000'),
(7, 3, 'C02G1003M4P1', 'IN_STOCK', '2026-03-02 08:00:00.000000'),
(7, 3, 'C02G1003M4P2', 'IN_STOCK', '2026-03-02 08:00:00.000000');

-- 11. Chương trình Thu cũ Đổi mới (Trade-In / Trợ giá lên đời)
INSERT INTO trade_in_devices (id, model, base_price, subsidy_amount, is_active) VALUES
(1, 'iPhone 13 Pro Max 128GB', 12500000, 1000000, TRUE),
(2, 'iPhone 14 Pro Max 128GB', 16500000, 1500000, TRUE),
(3, 'iPhone 15 Pro Max 256GB', 22000000, 2000000, TRUE),
(4, 'Samsung Galaxy S23 Ultra 256GB', 13500000, 1000000, TRUE),
(5, 'Samsung Galaxy S24 Ultra 256GB', 18500000, 1500000, TRUE);

INSERT INTO trade_in_criterias (id, name, deduction_percent) VALUES
(1, 'Loại 1: Thân máy đẹp như mới, pin > 85%, màn hình không trầy xước', 0),
(2, 'Loại 2: Thân máy trầy xước nhẹ viền, các chức năng hoạt động hoàn hảo', 10),
(3, 'Loại 3: Thân máy cấn móp nhẹ, pin chai hoặc màn xước dăm', 25),
(4, 'Loại 4: Máy lỗi chức năng phụ (FaceID/Vân tay), màn hình ám ố', 60);

-- 12. Gói trả góp 0% (Installment Plans)
INSERT INTO installment_plans (id, provider, months, fee_percent, is_active) VALUES
(1, 'Home Credit 0% Lãi Suất', 6, 0.00, TRUE),
(2, 'Home Credit 0% Lãi Suất', 12, 0.00, TRUE),
(3, 'FE Credit 0% Trả Góp', 6, 0.00, TRUE),
(4, 'FE Credit 0% Trả Góp', 9, 0.00, TRUE),
(5, 'Thẻ tín dụng Quốc tế (Visa/Mastercard) 0%', 3, 0.00, TRUE),
(6, 'Thẻ tín dụng Quốc tế (Visa/Mastercard) 0%', 6, 0.00, TRUE),
(7, 'Thẻ tín dụng Quốc tế (Visa/Mastercard) 0%', 12, 0.00, TRUE);

-- 13. Mã khuyến mãi & Giảm giá (Coupons)
INSERT INTO coupons (id, code, description, discount_type, discount_value, min_order_amount, max_discount_amount, max_uses, used_count, expires_at, status, version, created_at, updated_at) VALUES
(1, 'SMEMBER500K', 'Giảm 500.000 ₫ cho đơn hàng công nghệ từ 15.000.000 ₫', 'FIXED', 500000, 15000000, NULL, 500, 12, '2026-12-31 23:59:59.000000', 'ACTIVE', 0, '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000'),
(2, 'TECH10', 'Giảm 10% tối đa 200.000 ₫ cho phụ kiện từ 500.000 ₫', 'PERCENT', 10, 500000, 200000, 1000, 45, '2026-12-31 23:59:59.000000', 'ACTIVE', 0, '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000'),
(3, 'SALE50K', 'Giảm trực tiếp 50.000 ₫ cho đơn hàng từ 300.000 ₫', 'FIXED', 50000, 300000, NULL, 500, 8, '2026-12-31 23:59:59.000000', 'ACTIVE', 0, '2026-01-01 00:00:00.000000', '2026-01-01 00:00:00.000000');

-- 14. Đánh giá sản phẩm từ khách hàng thực tế (Product Reviews)
INSERT INTO product_reviews (product_id, user_id, author_name, rating, comment, status, version, created_at, updated_at) VALUES
(1, 2, 'Nguyễn Văn An', 5, 'Màu Titan Sa Mạc bên ngoài sang trọng hơn hẳn trên ảnh! Màn hình 6.9 inch viền siêu mỏng, quay video 4K 120fps mượt không tưởng. Đặt online ghé shop Thái Hà nhận máy trong 15 phút rất chuyên nghiệp.', 'APPROVED', 0, '2026-02-15 14:20:00.000000', '2026-02-15 14:20:00.000000'),
(1, 3, 'Trần Hoàng Long', 5, 'Máy cầm rất đầm tay, viền titanium không bám vân tay nhiều như thép. Pin trâu dùng cả ngày từ sáng đến tối vẫn còn hơn 35%. Hài lòng với dịch vụ TechShop!', 'APPROVED', 0, '2026-02-20 16:45:00.000000', '2026-02-20 16:45:00.000000'),
(2, 2, 'Nguyễn Văn An', 5, 'Galaxy S25 Ultra màn hình phẳng hoàn toàn dán cường lực cực dễ. Bút S-Pen tiện ghi chú trong cuộc họp. Camera zoom quang học chụp sân khấu cực kỳ nét.', 'APPROVED', 0, '2026-02-25 11:10:00.000000', '2026-02-25 11:10:00.000000'),
(3, 3, 'Trần Hoàng Long', 5, 'MacBook Pro M4 Pro chạy render video 4K Premiere Pro và After Effects mát rượi, quạt hầu như không thèm quay. Màn hình Liquid Retina XDR độ chuẩn màu cực cao.', 'APPROVED', 0, '2026-03-01 09:30:00.000000', '2026-03-01 09:30:00.000000');

-- ==============================================================================
-- HOÀN TẤT SEED DỮ LIỆU
-- ==============================================================================
SELECT 'Seed dữ liệu TechShop hoàn tất thành công!' AS result, 
       (SELECT COUNT(*) FROM products) AS total_products,
       (SELECT COUNT(*) FROM product_variants) AS total_variants,
       (SELECT COUNT(*) FROM product_items_imei) AS total_imei,
       (SELECT COUNT(*) FROM store_branches) AS total_branches;
