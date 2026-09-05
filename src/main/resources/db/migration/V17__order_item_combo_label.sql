-- What the item priced as (ALMUERZO_COMPLETO, ALMUERZO_BANDEJA, SOLO_SOPA, SUELTOS, ...),
-- persisted at pricing time alongside the breakdown from V16, for the same reason:
-- stays accurate even if price settings change later.
ALTER TABLE order_items ADD COLUMN combo_label VARCHAR(30);
