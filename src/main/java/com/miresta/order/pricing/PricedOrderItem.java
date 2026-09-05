package com.miresta.order.pricing;

import com.miresta.shared.Money;

/**
 * The full price breakdown for one order item, so a later "why does this cost this
 * much" view doesn't have to re-derive it — each field is a named bucket that summed
 * together (plus toGoSurcharge) equals total.
 *
 * @param comboLabel Which combo (if any) this priced as — e.g. "ALMUERZO_COMPLETO",
 *                   "ALMUERZO_BANDEJA" — or, when no combo formed, "SOLO_SOPA" /
 *                   "SOLO_PROTEINA" / "SOLO_ACOMPANANTE" / "SUELTOS", or null when
 *                   there's nothing here worth naming (e.g. just a drink).
 */
public record PricedOrderItem(
        String comboLabel,
        Money baseTotal,
        Money drinksTotal,
        Money proteinAdditionalsTotal,
        Money sideAdditionalsTotal,
        Money extrasTotal,
        Money individualsTotal,
        Money toGoSurcharge,
        Money total) {
}
