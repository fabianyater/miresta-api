package com.miresta.shared;

/**
 * The "role" a product/selection occupies within a combo (bandeja), regardless of
 * which concrete product fills it. Mirrors the catalog's {@code Category.code}.
 */
public enum ComboCategory {
    SOPA,
    PRINCIPIO,
    PROTEINA,
    ACOMPANANTE,
    ADICIONAL,
    BEBIDA,
    ESPECIAL,
    ENVASE
}
