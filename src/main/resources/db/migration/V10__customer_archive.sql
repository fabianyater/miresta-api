-- Lets a customer be archived (hidden from active use, e.g. the order picker) instead
-- of deleted, so their order history stays intact. True deletion is only allowed
-- when they have no orders at all (enforced by the existing FK, not schema here).
ALTER TABLE customers ADD COLUMN active BOOLEAN NOT NULL DEFAULT true;
