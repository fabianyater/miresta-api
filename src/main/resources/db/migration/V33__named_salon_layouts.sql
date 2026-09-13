-- Varios planos guardados por salón (antes solo uno) — cada uno con nombre propio,
-- para poder guardar distintos acomodos (ej. "Fin de semana", "Evento") y elegir cuál aplicar.
ALTER TABLE salon_layouts DROP CONSTRAINT salon_layouts_salon_id_key;
ALTER TABLE salon_layouts ADD COLUMN name TEXT;
UPDATE salon_layouts SET name = 'Plano guardado' WHERE name IS NULL;
ALTER TABLE salon_layouts ALTER COLUMN name SET NOT NULL;
ALTER TABLE salon_layouts ADD CONSTRAINT uq_salon_layouts_salon_name UNIQUE (salon_id, name);
