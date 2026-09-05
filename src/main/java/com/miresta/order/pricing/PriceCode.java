package com.miresta.order.pricing;

/**
 * Every price the business can change from the app. Using an enum instead of a raw
 * string key rules out typos silently creating a "new" price nobody edits.
 */
public enum PriceCode {
    DESAYUNO_COMPLETO,
    DESAYUNO_BANDEJA,
    DESAYUNO_CALDO_SUELTO,
    DESAYUNO_ACOMPANANTE_ADICION,
    DESAYUNO_PROTEINA_ADICION,
    DESAYUNO_PROTEINA_SUELTA,
    DESAYUNO_COMPONENTE_SUELTO,

    ALMUERZO_COMPLETO,
    ALMUERZO_BANDEJA,
    ALMUERZO_SOPA_SUELTA,
    ALMUERZO_ACOMPANANTE_ADICION,
    ALMUERZO_PROTEINA_ADICION,
    ALMUERZO_PROTEINA_SUELTA,
    ALMUERZO_COMPONENTE_SUELTO,

    ESPECIAL_COMPLETO,
    ESPECIAL_ACOMPANANTE_ADICION,
    ESPECIAL_PROTEINA_ADICION,
    ESPECIAL_COMPONENTE_SUELTO,

    BEBIDA_DEFAULT,
    BEBIDA_PERSONAL,
    BEBIDA_COCA_COLA_PERSONAL,
    /** @deprecated no longer matched by name (see PricingCalculator) — kept only so
     *  existing rows/history aren't orphaned; safe to delete from Precios if unused. */
    @Deprecated
    BEBIDA_COCA_COLA_1_5,

    /**
     * Global, meal-type-independent container costs — the whole "para llevar" surcharge
     * for a combo is just the sum of whichever of these it actually needs (see
     * PricingCalculator: needsSopaContainer / needsBandejaContainer), instead of a
     * separately hand-tuned "amount to go" repeated (and drifting) on every combo code.
     */
    ENVASE_SOPA,
    ENVASE_BANDEJA
}
