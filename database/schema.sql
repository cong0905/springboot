-- TechShop v0.1 · REFERENCE DESIGN, not an executed Flyway migration.
-- Target: MySQL 8.4 / InnoDB / utf8mb4. Timestamps supplied by app as UTC.
-- Create/select an empty database before execution; do not run on live data.
-- Product begins at zero stock. Every stock change requires a ledger row.

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

CREATE TABLE product_images (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  asset_path VARCHAR(255) NOT NULL,
  alt_text VARCHAR(200) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_images_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT ck_images_sort CHECK (sort_order >= 0),
  INDEX ix_images_product (product_id, sort_order, id)
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
  quantity INT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id),
  CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT uq_cart_items_product UNIQUE (cart_id, product_id),
  CONSTRAINT ck_cart_items_quantity CHECK (quantity BETWEEN 1 AND 99)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE orders (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  checkout_key CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  request_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  status VARCHAR(16) NOT NULL,
  recipient_name VARCHAR(100) NOT NULL,
  phone CHAR(10) NOT NULL,
  address VARCHAR(500) NOT NULL,
  note VARCHAR(500) NULL,
  currency CHAR(3) NOT NULL DEFAULT 'VND',
  subtotal DECIMAL(15,0) NOT NULL,
  shipping_fee DECIMAL(15,0) NOT NULL,
  total_amount DECIMAL(15,0) NOT NULL,
  cod_collected BOOLEAN NOT NULL DEFAULT FALSE,
  delivered_at DATETIME(6) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT uq_orders_checkout UNIQUE (user_id, checkout_key),
  CONSTRAINT ck_orders_status CHECK (status IN ('PENDING','CONFIRMED','SHIPPED','DELIVERED','CANCELLED')),
  CONSTRAINT ck_orders_currency CHECK (currency = 'VND'),
  CONSTRAINT ck_orders_amounts CHECK (subtotal >= 0 AND shipping_fee >= 0 AND total_amount = subtotal + shipping_fee),
  CONSTRAINT ck_orders_version CHECK (version >= 0),
  CONSTRAINT ck_orders_cod CHECK (
    (status = 'DELIVERED' AND delivered_at IS NOT NULL AND cod_collected = TRUE)
    OR (status <> 'DELIVERED' AND delivered_at IS NULL AND cod_collected = FALSE)
  ),
  INDEX ix_orders_owner (user_id, created_at, id),
  INDEX ix_orders_status (status, created_at, id),
  INDEX ix_orders_revenue (status, delivered_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_items (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  sku_snapshot VARCHAR(50) NOT NULL,
  name_snapshot VARCHAR(200) NOT NULL,
  unit_price DECIMAL(15,0) NOT NULL,
  quantity INT NOT NULL,
  line_total DECIMAL(15,0) NOT NULL,
  CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT uq_order_items_product UNIQUE (order_id, product_id),
  CONSTRAINT ck_order_items_quantity CHECK (quantity BETWEEN 1 AND 99),
  CONSTRAINT ck_order_items_amount CHECK (unit_price >= 0 AND line_total = unit_price * quantity),
  INDEX ix_order_items_product (product_id, order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE order_status_history (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  actor_user_id BIGINT NOT NULL,
  from_status VARCHAR(16) NULL,
  to_status VARCHAR(16) NOT NULL,
  reason VARCHAR(500) NULL,
  order_version BIGINT NOT NULL,
  occurred_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_history_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_history_actor FOREIGN KEY (actor_user_id) REFERENCES users(id),
  CONSTRAINT uq_history_version UNIQUE (order_id, order_version),
  CONSTRAINT ck_history_from CHECK (from_status IS NULL OR from_status IN ('PENDING','CONFIRMED','SHIPPED','DELIVERED','CANCELLED')),
  CONSTRAINT ck_history_to CHECK (to_status IN ('PENDING','CONFIRMED','SHIPPED','DELIVERED','CANCELLED')),
  CONSTRAINT ck_history_version CHECK (order_version >= 0),
  INDEX ix_history_timeline (order_id, occurred_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE stock_movements (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  actor_user_id BIGINT NOT NULL,
  order_item_id BIGINT NULL,
  movement_type VARCHAR(16) NOT NULL,
  delta INT NOT NULL,
  balance_after INT NOT NULL,
  reason VARCHAR(500) NOT NULL,
  occurred_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_stock_product FOREIGN KEY (product_id) REFERENCES products(id),
  CONSTRAINT fk_stock_actor FOREIGN KEY (actor_user_id) REFERENCES users(id),
  CONSTRAINT fk_stock_order_item FOREIGN KEY (order_item_id) REFERENCES order_items(id),
  CONSTRAINT uq_stock_item_type UNIQUE (order_item_id, movement_type),
  CONSTRAINT ck_stock_balance CHECK (balance_after >= 0),
  CONSTRAINT ck_stock_type_delta CHECK (
    (movement_type = 'ADJUSTMENT' AND order_item_id IS NULL AND delta <> 0)
    OR (movement_type = 'ORDER' AND order_item_id IS NOT NULL AND delta < 0)
    OR (movement_type = 'CANCEL' AND order_item_id IS NOT NULL AND delta > 0)
  ),
  INDEX ix_stock_ledger (product_id, occurred_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
