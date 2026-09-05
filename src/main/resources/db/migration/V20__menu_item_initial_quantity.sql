-- Editing today's menu used to wipe out whatever stock had already been sold (it
-- deleted and recreated every menu_item row from the submitted quantities, which are
-- the REMAINING count, not the original cap). Tracking the original cap separately
-- lets an edit recompute "how much is left" from what's actually been consumed,
-- instead of resetting it.
ALTER TABLE menu_item
    ADD COLUMN initial_quantity INTEGER;

UPDATE menu_item SET initial_quantity = quantity;
