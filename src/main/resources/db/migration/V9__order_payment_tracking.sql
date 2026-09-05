-- Tracks when an order was actually paid, separate from order_status. This lets an
-- order be COMPLETED (served, table freed) while still unpaid — an open tab ("fiado")
-- billed to a registered customer, settled later.
ALTER TABLE orders ADD COLUMN paid_at TIMESTAMP;

-- Backfill: historically COMPLETED meant "paid at that moment" (no tabs existed yet).
UPDATE orders
SET paid_at = created_at
WHERE order_status_id = (SELECT id FROM order_status WHERE name = 'COMPLETED');
