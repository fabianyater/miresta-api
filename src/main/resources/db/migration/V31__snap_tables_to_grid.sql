-- El plano ahora es una cuadrícula fija de 8x5 casillas por salón (antes las mesas
-- quedaron en una cuadrícula suelta de 4 columnas con separación fija) — se reacomodan
-- al centro de su casilla en la nueva cuadrícula, en el mismo orden relativo de antes.
WITH numbered AS (
    SELECT id, salon_id,
           row_number() OVER (PARTITION BY salon_id ORDER BY number) - 1 AS idx
    FROM dining_table
)
UPDATE dining_table t
SET position_x = (100.0 * ((n.idx % 8) + 0.5)) / 8,
    position_y = (100.0 * (((n.idx / 8) % 5) + 0.5)) / 5
FROM numbered n
WHERE n.id = t.id;
