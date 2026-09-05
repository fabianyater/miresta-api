-- Lets a price setting actually be deleted: its history rows go with it instead of
-- blocking the delete with a FK violation.
ALTER TABLE price_setting_history DROP CONSTRAINT price_setting_history_price_setting_id_fkey;
ALTER TABLE price_setting_history
    ADD CONSTRAINT price_setting_history_price_setting_id_fkey
    FOREIGN KEY (price_setting_id) REFERENCES price_settings (id) ON DELETE CASCADE;
