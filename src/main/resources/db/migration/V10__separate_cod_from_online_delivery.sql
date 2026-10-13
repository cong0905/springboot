ALTER TABLE orders DROP CHECK ck_orders_cod;
ALTER TABLE orders ADD CONSTRAINT ck_orders_cod_v10 CHECK (
  (status = 'DELIVERED' AND delivered_at IS NOT NULL AND (
    (payment_method = 'COD' AND cod_collected = TRUE)
    OR (payment_method <> 'COD' AND cod_collected = FALSE)
  ))
  OR (status <> 'DELIVERED' AND delivered_at IS NULL AND cod_collected = FALSE)
);
CREATE INDEX ix_orders_hold_expiry ON orders(status, payment_status, hold_expires_at);
