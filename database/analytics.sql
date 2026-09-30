-- TechShop v0.1 · reference reporting queries, not run on MySQL yet.
-- Set UTC parameters per request. Example: Vietnamese day 2026-10-01.
SET @start_utc = '2026-09-30 17:00:00';
SET @end_utc = '2026-10-01 17:00:00';

-- Q-01: delivered revenue. Aggregate orders alone to avoid item fanout.
SELECT COUNT(*) AS delivered_orders,
       COALESCE(SUM(subtotal), 0) AS merchandise_revenue,
       COALESCE(SUM(shipping_fee), 0) AS shipping_collected,
       COALESCE(SUM(total_amount), 0) AS cod_collected_total,
       CASE WHEN COUNT(*) = 0 THEN NULL
            ELSE ROUND(SUM(subtotal) / COUNT(*), 0) END AS aov
FROM orders
WHERE status = 'DELIVERED'
  AND delivered_at >= @start_utc AND delivered_at < @end_utc;

-- Q-02: placed orders; this cohort differs from delivered orders above.
SELECT COUNT(*) AS placed_orders
FROM orders
WHERE created_at >= @start_utc AND created_at < @end_utc;

-- Q-03: top products by delivered units; revenue uses item snapshot price.
SELECT p.id AS product_id, p.sku, p.name,
       SUM(oi.quantity) AS units_sold,
       SUM(oi.line_total) AS merchandise_revenue
FROM order_items oi
JOIN orders o ON o.id = oi.order_id
JOIN products p ON p.id = oi.product_id
WHERE o.status = 'DELIVERED'
  AND o.delivered_at >= @start_utc AND o.delivered_at < @end_utc
GROUP BY p.id, p.sku, p.name
ORDER BY units_sold DESC, merchandise_revenue DESC, p.id ASC
LIMIT 10;

-- Q-04: daily revenue by Vietnam day (UTC+7, no DST in this baseline).
-- App fills absent days with zero. WHERE keeps delivered_at indexable.
SELECT DATE(delivered_at + INTERVAL 7 HOUR) AS vietnam_date,
       COUNT(*) AS delivered_orders,
       SUM(subtotal) AS merchandise_revenue
FROM orders
WHERE status = 'DELIVERED'
  AND delivered_at >= @start_utc AND delivered_at < @end_utc
GROUP BY DATE(delivered_at + INTERVAL 7 HOUR)
ORDER BY vietnam_date;

-- Q-05: current operational states, NOT period transition counts.
SELECT status, COUNT(*) AS order_count
FROM orders GROUP BY status ORDER BY status;

-- Q-06: low sellable stock snapshot. Threshold 5 is a working assumption.
SELECT p.id, p.sku, p.name, p.stock_quantity
FROM products p JOIN categories c ON c.id = p.category_id
WHERE p.status = 'ACTIVE' AND c.status = 'ACTIVE' AND p.stock_quantity <= 5
ORDER BY p.stock_quantity, p.id;

-- Q-07: average elapsed delivery hours, not a committed shipping SLA.
SELECT AVG(TIMESTAMPDIFF(SECOND, created_at, delivered_at)) / 3600.0
       AS average_delivery_hours
FROM orders
WHERE status = 'DELIVERED'
  AND delivered_at >= @start_utc AND delivered_at < @end_utc;

-- DQ-01: each query below should return zero violation rows.
SELECT o.id, o.subtotal, COALESCE(SUM(oi.line_total), 0) AS items_total
FROM orders o LEFT JOIN order_items oi ON oi.order_id = o.id
GROUP BY o.id, o.subtotal
HAVING COUNT(oi.id) = 0 OR o.subtotal <> COALESCE(SUM(oi.line_total), 0);

-- DQ-02: zero initial stock and complete ledger are required assumptions.
SELECT p.id, p.stock_quantity, COALESCE(SUM(sm.delta), 0) AS ledger_balance
FROM products p LEFT JOIN stock_movements sm ON sm.product_id = p.id
GROUP BY p.id, p.stock_quantity
HAVING p.stock_quantity <> COALESCE(SUM(sm.delta), 0);

-- DQ-03: order debit/release links and deltas must match the actual item.
SELECT oi.id AS order_item_id, o.status,
       debit.delta AS debit_delta, release_movement.delta AS release_delta
FROM order_items oi
JOIN orders o ON o.id = oi.order_id
LEFT JOIN stock_movements debit
  ON debit.order_item_id = oi.id AND debit.movement_type = 'ORDER'
LEFT JOIN stock_movements release_movement
  ON release_movement.order_item_id = oi.id AND release_movement.movement_type = 'CANCEL'
WHERE debit.id IS NULL OR debit.delta <> -oi.quantity
   OR debit.product_id <> oi.product_id
   OR (o.status = 'CANCELLED' AND
       (release_movement.id IS NULL OR release_movement.delta <> oi.quantity
        OR release_movement.product_id <> oi.product_id))
   OR (o.status <> 'CANCELLED' AND release_movement.id IS NOT NULL);

-- DQ-04: lifecycle, money and COD invariants (also covered by CHECKs).
SELECT id FROM orders
WHERE total_amount <> subtotal + shipping_fee
   OR (status = 'DELIVERED' AND (delivered_at IS NULL OR cod_collected <> TRUE))
   OR (status <> 'DELIVERED' AND (delivered_at IS NOT NULL OR cod_collected <> FALSE));

-- DQ-05: latest history matches current order status/version.
SELECT o.id, o.status, o.version
FROM orders o
LEFT JOIN order_status_history h
  ON h.order_id = o.id AND h.order_version = o.version
WHERE h.id IS NULL OR h.to_status <> o.status;
