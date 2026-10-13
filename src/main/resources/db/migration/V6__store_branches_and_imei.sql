CREATE TABLE store_branches (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL,
 code VARCHAR(50) NOT NULL UNIQUE, address VARCHAR(500) NOT NULL,
 province_code VARCHAR(20) NOT NULL, district_code VARCHAR(20), ward_code VARCHAR(20),
 phone VARCHAR(20) NOT NULL, latitude DOUBLE NOT NULL, longitude DOUBLE NOT NULL,
 is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE branch_inventories (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, branch_id BIGINT NOT NULL, variant_id BIGINT NOT NULL,
 quantity INT NOT NULL DEFAULT 0, reserved_quantity INT NOT NULL DEFAULT 0, version BIGINT NOT NULL DEFAULT 0,
 FOREIGN KEY(branch_id) REFERENCES store_branches(id),
 FOREIGN KEY(variant_id) REFERENCES product_variants(id) ON DELETE CASCADE,
 UNIQUE(branch_id,variant_id), CHECK(quantity >= 0 AND reserved_quantity >= 0 AND reserved_quantity <= quantity)
);
CREATE TABLE product_items_imei (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, variant_id BIGINT NOT NULL, branch_id BIGINT NOT NULL,
 imei_number VARCHAR(40) NOT NULL UNIQUE, status VARCHAR(16) NOT NULL,
 order_item_id BIGINT NULL, imported_at DATETIME(6) NOT NULL, sold_at DATETIME(6),
 FOREIGN KEY(variant_id) REFERENCES product_variants(id) ON DELETE CASCADE,
 FOREIGN KEY(branch_id) REFERENCES store_branches(id),
 FOREIGN KEY(order_item_id) REFERENCES order_items(id),
 CHECK(status IN ('IN_STOCK','RESERVED','SOLD','DEFECTIVE','RETURNED')),
 INDEX ix_imei_stock(variant_id,branch_id,status,id)
);
CREATE TABLE imei_history (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, imei_id BIGINT NOT NULL, from_status VARCHAR(16),
 to_status VARCHAR(16) NOT NULL, from_branch_id BIGINT, to_branch_id BIGINT NOT NULL,
 reason VARCHAR(500) NOT NULL, occurred_at DATETIME(6) NOT NULL,
 FOREIGN KEY(imei_id) REFERENCES product_items_imei(id) ON DELETE CASCADE
);
