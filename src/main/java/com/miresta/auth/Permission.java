package com.miresta.auth;

/**
 * Catálogo fijo de acciones que se pueden habilitar/deshabilitar por rol — el código
 * (este enum) es la única fuente de verdad de qué permisos existen; cuál rol los
 * tiene hoy es dato editable (ver {@link RolePermission}), no código. OWNER siempre
 * tiene todos, sin excepción — ver {@link AccessService#has}.
 */
public enum Permission {
    MESAS_VER("Mesas", "Ver mesas y su estado"),
    MESAS_EDITAR("Mesas", "Crear, mover de salón y eliminar mesas"),
    MESAS_UNIR("Mesas", "Unir y separar mesas"),

    SALONES_VER("Salones", "Ver salones y su plano"),
    SALONES_EDITAR("Salones", "Crear/editar salones, acomodar el plano y sus planos guardados"),

    CATALOGO_VER("Catálogo", "Ver categorías, productos y lotes/stock"),
    CATALOGO_EDITAR("Catálogo", "Crear/editar categorías y productos, registrar lotes"),

    CLIENTES_VER("Clientes", "Ver, buscar y registrar clientes"),
    CLIENTES_EDITAR("Clientes", "Editar o eliminar clientes"),

    MENU_VER("Menú del día", "Ver el menú del día"),
    MENU_EDITAR("Menú del día", "Crear o eliminar el menú del día"),

    PEDIDOS_CREAR("Pedidos", "Tomar pedidos, verlos e imprimir comanda/cuenta"),
    PEDIDOS_COBRAR("Pedidos", "Cobrar, fiar, liquidar saldos y ver cuánto debe cada cliente"),
    PEDIDOS_REPORTES("Pedidos", "Ver reportes agregados de pagos e imprimir el resumen del día"),

    CAJA_VER("Caja", "Ver turnos y movimientos de caja"),
    CAJA_EDITAR("Caja", "Abrir/cerrar turno y registrar movimientos de caja"),

    PRECIOS_VER("Precios", "Ver los precios vigentes y su historial"),
    PRECIOS_EDITAR("Precios", "Editar precios"),

    IMPRESORA_CONFIG("Impresora", "Configurar la impresora (nombre, encabezado, papel, reintentos)"),

    COCINA_VER("Cocina", "Ver/enviar mensajes a cocina y ver las frases guardadas"),
    COCINA_FRASES_EDITAR("Cocina", "Editar la lista de frases de cocina"),

    USUARIOS_VER("Usuarios", "Ver la lista de usuarios"),
    USUARIOS_EDITAR("Usuarios", "Crear, editar o eliminar usuarios");

    private final String domain;
    private final String description;

    Permission(String domain, String description) {
        this.domain = domain;
        this.description = description;
    }

    public String domain() {
        return domain;
    }

    public String description() {
        return description;
    }
}
