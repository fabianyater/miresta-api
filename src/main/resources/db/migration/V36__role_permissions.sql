CREATE TABLE role_permission (
    role       TEXT NOT NULL,
    permission TEXT NOT NULL,
    PRIMARY KEY (role, permission)
);

-- OWNER no tiene filas: siempre tiene todos los permisos (ver AccessService).
-- ADMIN: reproduce el acceso actual de hasAnyRole('ADMIN','OWNER') en todos los controllers.
INSERT INTO role_permission (role, permission) VALUES
('ADMIN', 'MESAS_VER'),
('ADMIN', 'MESAS_EDITAR'),
('ADMIN', 'MESAS_UNIR'),
('ADMIN', 'SALONES_VER'),
('ADMIN', 'SALONES_EDITAR'),
('ADMIN', 'CATALOGO_VER'),
('ADMIN', 'CATALOGO_EDITAR'),
('ADMIN', 'CLIENTES_VER'),
('ADMIN', 'CLIENTES_EDITAR'),
('ADMIN', 'MENU_VER'),
('ADMIN', 'MENU_EDITAR'),
('ADMIN', 'PEDIDOS_CREAR'),
('ADMIN', 'PEDIDOS_COBRAR'),
('ADMIN', 'PEDIDOS_REPORTES'),
('ADMIN', 'CAJA_VER'),
('ADMIN', 'CAJA_EDITAR'),
('ADMIN', 'PRECIOS_VER'),
('ADMIN', 'PRECIOS_EDITAR'),
('ADMIN', 'IMPRESORA_CONFIG'),
('ADMIN', 'COCINA_VER'),
('ADMIN', 'COCINA_FRASES_EDITAR'),
('ADMIN', 'USUARIOS_VER'),
('ADMIN', 'USUARIOS_EDITAR');

-- MESERO: reproduce el acceso actual (operativo + unir mesas, sin caja/precios/administración).
INSERT INTO role_permission (role, permission) VALUES
('MESERO', 'MESAS_VER'),
('MESERO', 'MESAS_UNIR'),
('MESERO', 'SALONES_VER'),
('MESERO', 'CATALOGO_VER'),
('MESERO', 'CLIENTES_VER'),
('MESERO', 'MENU_VER'),
('MESERO', 'PEDIDOS_CREAR'),
('MESERO', 'PEDIDOS_COBRAR'),
('MESERO', 'COCINA_VER');
