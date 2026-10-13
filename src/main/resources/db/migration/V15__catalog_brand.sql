ALTER TABLE products ADD COLUMN brand VARCHAR(80) NULL;
CREATE INDEX idx_products_brand ON products(brand,status);
UPDATE products p SET brand=(SELECT MAX(s.spec_value) FROM product_specifications s WHERE s.product_id=p.id AND s.spec_group='Thông tin' AND s.spec_name='Thương hiệu') WHERE p.brand IS NULL;
