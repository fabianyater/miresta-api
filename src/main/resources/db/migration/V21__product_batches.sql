CREATE TABLE product_batches (
    id                 BIGSERIAL PRIMARY KEY,
    product_id         BIGINT       NOT NULL REFERENCES products (id),
    quantity_received  INTEGER      NOT NULL,
    quantity_remaining INTEGER      NOT NULL,
    expiration_date    DATE,
    received_at        DATE         NOT NULL DEFAULT CURRENT_DATE,
    created_at         TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_product_batches_product_id ON product_batches (product_id);

-- Migra lo que ya hubiera en product_details (un solo "lote" heredado, sin historial)
-- a la nueva tabla, para no perder cantidades/vencimientos ya cargados.
INSERT INTO product_batches (product_id, quantity_received, quantity_remaining, expiration_date, received_at)
SELECT product_id, quantity, quantity, expiration_date, CURRENT_DATE
FROM product_details
WHERE quantity IS NOT NULL
  AND quantity > 0;

ALTER TABLE product_details
    DROP COLUMN quantity,
    DROP COLUMN expiration_date;
