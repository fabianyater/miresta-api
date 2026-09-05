-- Each plato within an order can now carry its own customer (nullable) — a table's
-- pending order can mix platos for different people, and each one needs to be
-- fiado/billed independently instead of the whole ticket always going to one customer.
ALTER TABLE order_items
    ADD COLUMN customer_id BIGINT REFERENCES customers (id);
