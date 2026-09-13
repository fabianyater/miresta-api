-- Más configuración de impresora: qué se imprime en el encabezado/pie de los tickets,
-- ancho de papel y corte automático, y reintentos/timeout si falla el envío.
ALTER TABLE printer_setting ADD COLUMN header_line_1 TEXT;
ALTER TABLE printer_setting ADD COLUMN header_line_2 TEXT;
ALTER TABLE printer_setting ADD COLUMN address_line TEXT;
ALTER TABLE printer_setting ADD COLUMN footer_message TEXT;
ALTER TABLE printer_setting ADD COLUMN paper_width_chars INTEGER NOT NULL DEFAULT 32;
ALTER TABLE printer_setting ADD COLUMN auto_cut BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE printer_setting ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 2;
ALTER TABLE printer_setting ADD COLUMN timeout_seconds INTEGER NOT NULL DEFAULT 10;

-- Mismo texto que hoy sale fijo en el código, para no cambiar nada visualmente al
-- migrar — de aquí en adelante es editable desde Admin → Impresora.
UPDATE printer_setting
SET header_line_1 = 'Restaurante Tradición',
    header_line_2 = 'Leña y Carbón',
    footer_message = '¡Gracias por su visita!'
WHERE header_line_1 IS NULL;

ALTER TABLE printer_setting ALTER COLUMN header_line_1 SET NOT NULL;
