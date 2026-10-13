CREATE TABLE product_variants (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, product_id BIGINT NOT NULL,
 sku_code VARCHAR(50) NOT NULL UNIQUE, color_name VARCHAR(80) NOT NULL,
 storage_capacity VARCHAR(80) NOT NULL, price DECIMAL(15,0) NOT NULL,
 original_price DECIMAL(15,0) NOT NULL, status VARCHAR(16) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_variant_product FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE,
 CONSTRAINT ck_variant_price CHECK(price >= 0 AND original_price >= price)
);
CREATE TABLE product_specifications (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, product_id BIGINT NOT NULL,
 spec_group VARCHAR(80) NOT NULL, spec_name VARCHAR(80) NOT NULL,
 spec_value VARCHAR(500) NOT NULL, display_order INT NOT NULL DEFAULT 0,
 CONSTRAINT fk_spec_product FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE,
 UNIQUE(product_id,spec_group,spec_name)
);
ALTER TABLE product_images ADD COLUMN variant_id BIGINT NULL;
ALTER TABLE product_images ADD CONSTRAINT fk_image_variant FOREIGN KEY(variant_id) REFERENCES product_variants(id) ON DELETE CASCADE;
ALTER TABLE cart_items ADD COLUMN variant_id BIGINT NULL;
ALTER TABLE cart_items ADD CONSTRAINT fk_cart_variant FOREIGN KEY(variant_id) REFERENCES product_variants(id);
ALTER TABLE cart_items DROP CONSTRAINT uq_cart_items_product;
ALTER TABLE cart_items ADD CONSTRAINT uq_cart_variant UNIQUE(cart_id,product_id,variant_id);
ALTER TABLE order_items ADD COLUMN variant_id BIGINT NULL;
ALTER TABLE order_items DROP CONSTRAINT uq_order_items_product;
ALTER TABLE order_items ADD CONSTRAINT uq_order_variant UNIQUE(order_id,product_id,variant_id);
