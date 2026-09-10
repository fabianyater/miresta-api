-- Nombre real del usuario, para identificar quién tomó cada pedido (en la comanda
-- impresa y en la lista de Pedidos). `name` es el nombre completo; `display_name`
-- es el nombre corto que se muestra en el badge y se imprime en el ticket.
ALTER TABLE users ADD COLUMN name         TEXT NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN display_name TEXT NOT NULL DEFAULT '';

-- Backfill de las cuentas que ya existen: se deriva de la parte local del correo
-- hasta que alguien lo edite en Admin -> Usuarios.
UPDATE users
SET name         = split_part(email, '@', 1),
    display_name = initcap(split_part(split_part(email, '@', 1), '.', 1))
WHERE name = '';
