-- Unir mesas: una mesa "secundaria" apunta a su mesa "principal" mientras dure el
-- grupo — el pedido/cuenta sigue siendo el de la principal, las secundarias no
-- generan uno propio. Se disuelve solo cuando la principal vuelve a quedar libre
-- (ver TableServiceImpl.updateTableStatus).
ALTER TABLE dining_table ADD COLUMN merged_into_id BIGINT REFERENCES dining_table (id) ON DELETE SET NULL;

CREATE INDEX idx_dining_table_merged_into ON dining_table (merged_into_id);
