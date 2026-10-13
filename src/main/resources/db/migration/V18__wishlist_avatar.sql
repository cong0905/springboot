ALTER TABLE users ADD COLUMN avatar_path VARCHAR(255) NULL;
CREATE TABLE wishlists (
 user_id BIGINT NOT NULL, product_id BIGINT NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 PRIMARY KEY(user_id,product_id),
 CONSTRAINT fk_wishlist_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
 CONSTRAINT fk_wishlist_product FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
);
