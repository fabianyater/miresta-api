CREATE TABLE order_payments
(
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT    NOT NULL REFERENCES orders (id),
    payment_type_id BIGINT    NOT NULL REFERENCES payment_type (id),
    amount          BIGINT    NOT NULL,
    paid_at         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_order_payments_order_id ON order_payments (order_id);

-- Backfill: cada pedido ya pagado con un solo método se convierte en su primera
-- (única) línea de pago, para no perder el historial ya cobrado antes de esto.
INSERT INTO order_payments (order_id, payment_type_id, amount, paid_at)
SELECT id, payment_type_id, total, paid_at
FROM orders
WHERE paid_at IS NOT NULL
  AND payment_type_id IS NOT NULL;
