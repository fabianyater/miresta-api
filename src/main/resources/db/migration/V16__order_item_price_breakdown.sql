-- Persists the price breakdown at the moment an order item is priced, so viewing an
-- order later always shows exactly why it cost what it cost — even if price settings
-- change afterward (recomputing live against current prices could silently disagree
-- with the total actually charged).
ALTER TABLE order_items
    ADD COLUMN drinks_total              BIGINT,
    ADD COLUMN protein_additionals_total BIGINT,
    ADD COLUMN side_additionals_total    BIGINT,
    ADD COLUMN extras_total              BIGINT,
    ADD COLUMN individuals_total         BIGINT;
