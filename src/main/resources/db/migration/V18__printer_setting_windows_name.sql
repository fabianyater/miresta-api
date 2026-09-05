-- The printer turned out to be USB-only (Jaltech POS 80), not network. Printing now
-- goes through the Windows-shared printer queue by name instead of an IP:port socket.
ALTER TABLE printer_setting
    ADD COLUMN printer_name TEXT;

UPDATE printer_setting SET printer_name = 'JALTECH-POS-80';

ALTER TABLE printer_setting
    ALTER COLUMN printer_name SET NOT NULL,
    DROP COLUMN ip,
    DROP COLUMN port;
