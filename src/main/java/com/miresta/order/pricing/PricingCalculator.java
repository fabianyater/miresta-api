package com.miresta.order.pricing;

import com.miresta.catalog.Product;
import com.miresta.order.OrderItemSelection;
import com.miresta.shared.ComboCategory;
import com.miresta.shared.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The one place that turns a set of {@link OrderItemSelection}s into a price. Replaces
 * the logic that used to be duplicated (and drifting out of sync) between
 * OrderItemServiceImpl and OrderItemSelectionsImpl.
 */
@RequiredArgsConstructor
@Service
public class PricingCalculator {

    private static final int SIDES_REQUIRED = 2;

    /** Policy: does a replacement-for-soup (e.g. principio "por sopa") count as a real soup for combo pricing? */
    private static final boolean REPLACEMENT_COUNTS_AS_REAL_SOUP = false;

    private final PriceSettingService priceSettings;

    /**
     * Per-unit charge for one additional unit of a selection (used when a selection's
     * quantity is greater than one, e.g. two extra rice portions on the same line).
     */
    public Money unitExtraPriceFor(OrderItemSelection selection, String mealType) {
        long quantity = selection.getQuantity() != null ? selection.getQuantity() : 1L;
        if (quantity <= 1) {
            return Money.ZERO;
        }
        return unitAmount(effectiveCategory(selection), selection.getProduct(), mealType);
    }

    /**
     * The price attributable to one specific selection, when that's actually a
     * meaningful thing to show — a drink and an "adición" are always priced per
     * product regardless of what else is on the plate, and everything else is too
     * when the whole item didn't form a priced combo (comboFormed = false). When it
     * did form a combo, a sopa/principio/proteína/acompañante's price is just its
     * share of that combo's one flat price — not a separate number — so this
     * returns null for those, letting the combo's own total speak for the whole
     * group instead.
     */
    public Money lineTotalFor(OrderItemSelection selection, String mealType, boolean comboFormed) {
        Product product = selection.getProduct();
        ComboCategory category = product.getCategory().getCode();
        ComboCategory effective = effectiveCategory(selection);
        long qty = selection.getQuantity() != null ? selection.getQuantity() : 1L;

        if (category == ComboCategory.BEBIDA) {
            return unitAmount(ComboCategory.BEBIDA, product, mealType).times(qty);
        }
        if (category == ComboCategory.ADICIONAL && effective != ComboCategory.PROTEINA) {
            return unitAmount(ComboCategory.ADICIONAL, product, mealType).times(qty);
        }
        if (!comboFormed) {
            // Effective, not raw, category — a principio standing in for the soup ("por
            // sopa") ordered alone prices like a sopa suelta, not like a loose principio.
            return unitAmount(effective, product, mealType).times(qty);
        }
        return null;
    }

    public PricedOrderItem priceOrderItem(String mealType, boolean isToGo, List<OrderItemSelection> selections) {
        long soupCredits = 0, principleCredits = 0, sideCredits = 0, especialCredits = 0;
        Map<String, Long> proteinsByType = new HashMap<>();

        long extrasTotal = 0L;
        long drinksTotal = 0L;
        long additionalsTotal = 0L;

        boolean hasNonEggPrinciple = false;
        long eggAdditionals = 0L;

        long soupCreditsFromRealSoup = 0L;
        long soupCreditsFromReplacement = 0L;

        // Only PRINCIPIO-category selections count here — a principio filled via
        // replacement (e.g. huevo "por principio") is priced through the ADICIONAL
        // branch below instead, so it must not also be double-charged here.
        long principleCreditsFromReal = 0L;

        for (OrderItemSelection s : selections) {
            long qty = s.getQuantity() != null ? s.getQuantity() : 1;
            long extraPrice = s.getUnitExtraPrice() != null ? s.getUnitExtraPrice().amount() : 0L;

            Product product = s.getProduct();
            ComboCategory category = product.getCategory().getCode();
            String normalizedName = normalize(product.getName());
            boolean isEgg = normalizedName.startsWith("huevo");
            ComboCategory effective = effectiveCategory(s);
            ComboCategory replacement = effective == category ? null : effective;

            // PROTEINA and ADICIONAL selections are priced through their own dedicated
            // buckets below (proteinAdditionals / additionalsTotal), and ESPECIAL through
            // basePrice's own per-unit multiplier — adding their unitExtraPrice here too
            // would double-charge them.
            boolean pricedElsewhere = category == ComboCategory.PROTEINA || category == ComboCategory.ADICIONAL
                    || category == ComboCategory.ESPECIAL || replacement == ComboCategory.PROTEINA;
            if (extraPrice > 0 && !pricedElsewhere) {
                extrasTotal += extraPrice;
            }

            switch (category) {
                case SOPA -> {
                    soupCredits += qty;
                    soupCreditsFromRealSoup += qty;
                }
                case PRINCIPIO -> {
                    principleCredits += qty;
                    principleCreditsFromReal += qty;
                    if (!isEgg) hasNonEggPrinciple = true;
                }
                case ACOMPANANTE -> {
                    sideCredits += qty;
                    if (isEgg) {
                        proteinsByType.merge("huevo", qty, Long::sum);
                    }
                }
                case PROTEINA -> proteinsByType.merge(extractProteinType(normalizedName), qty, Long::sum);
                case ADICIONAL -> {
                    if (replacement != ComboCategory.PROTEINA) {
                        additionalsTotal += priceSettings.amountFor(acompananteAdicionCode(mealType)).amount() * qty;
                        if (isEgg) eggAdditionals += qty;
                    }
                }
                case BEBIDA -> drinksTotal += unitAmount(ComboCategory.BEBIDA, product, mealType).amount() * qty;
                // A product in the ESPECIAL catalog category (Bandeja paisa, Sancocho de
                // gallina, etc.) IS the whole plate by itself — unlike sopa/principio/
                // proteína/acompañante it doesn't need anything else picked alongside it
                // to be "a full meal", so it gets its own credit toward forming the combo.
                case ESPECIAL -> especialCredits += qty;
                default -> {
                }
            }

            if (replacement != null) {
                switch (replacement) {
                    case SOPA -> {
                        soupCredits += qty;
                        soupCreditsFromReplacement += qty;
                    }
                    case PRINCIPIO -> principleCredits += qty;
                    case PROTEINA -> proteinsByType.merge(extractProteinType(normalizedName), qty, Long::sum);
                    case ACOMPANANTE -> sideCredits += qty;
                    default -> {
                    }
                }
            }
        }

        long totalProteins = proteinsByType.values().stream().mapToLong(Long::longValue).sum();
        long effectiveSoupCredits = soupCreditsFromRealSoup
                + (REPLACEMENT_COUNTS_AS_REAL_SOUP ? soupCreditsFromReplacement : 0);
        // Separate from effectiveSoupCredits on purpose: whether something fills the
        // soup's *container* slot (needs its own envase para llevar) is a different
        // question from whether it bumps the combo up to COMPLETO pricing — a principio
        // "por sopa" still needs a container even though it deliberately doesn't count
        // toward the combo tier.
        long soupCreditsForContainer = soupCreditsFromRealSoup + soupCreditsFromReplacement;

        String normalizedMealType = mealType.toUpperCase();
        PriceCode comboCode = null;

        if ("ALMUERZO".equals(normalizedMealType)) {
            boolean hasBasicCombo = totalProteins >= 1 && (sideCredits + principleCredits) >= SIDES_REQUIRED;
            boolean full = effectiveSoupCredits >= 1 && hasBasicCombo;
            boolean tray = effectiveSoupCredits == 0 && hasBasicCombo;
            comboCode = full ? PriceCode.ALMUERZO_COMPLETO : tray ? PriceCode.ALMUERZO_BANDEJA : null;
        } else if ("DESAYUNO".equals(normalizedMealType)) {
            boolean full = effectiveSoupCredits >= 1 && totalProteins >= 1 && sideCredits >= SIDES_REQUIRED;
            boolean tray = effectiveSoupCredits == 0 && totalProteins >= 1 && sideCredits >= SIDES_REQUIRED;
            comboCode = full ? PriceCode.DESAYUNO_COMPLETO : tray ? PriceCode.DESAYUNO_BANDEJA : null;
        } else if ("ESPECIAL".equals(normalizedMealType)) {
            boolean hasComboWorthPlate = totalProteins >= 1 || effectiveSoupCredits >= 1 || especialCredits >= 1;
            comboCode = hasComboWorthPlate ? PriceCode.ESPECIAL_COMPLETO : null;
        }

        // "Para llevar" is priced by container, not by a separately hand-tuned amount per
        // combo. This applies whether or not the order actually formed a priced combo — a
        // lone sopa taken to go still needs its own container, so this is NOT gated on
        // comboCode (a prior version was, which meant "solo sopa" or "solo proteína" para
        // llevar got charged no container at all).
        //
        // The soup container is proportional to how many soups are actually going out
        // (each one is its own liquid container — 20 sopas sueltas need 20 envases), but
        // the tray container stays flat at one regardless of how many proteins are on it
        // (a plate with two proteins is still one physical box, not two).
        Money toGoSurcharge = Money.ZERO;
        if (isToGo) {
            long surcharge = 0L;
            if (soupCreditsForContainer >= 1) {
                surcharge += soupCreditsForContainer * priceSettings.amountFor(PriceCode.ENVASE_SOPA).amount();
            }
            if (totalProteins >= 1) {
                surcharge += priceSettings.amountFor(PriceCode.ENVASE_BANDEJA).amount();
            }
            toGoSurcharge = Money.of(surcharge);
        }
        // Each ESPECIAL-category unit ordered is its own full plate (2x Bandeja paisa =
        // two plates), unlike the other meal types where the combo price is always flat
        // per plato regardless of quantity — everything else there is a *component* of
        // one shared plate, not a standalone one.
        long especialMultiplier = "ESPECIAL".equals(normalizedMealType) ? Math.max(especialCredits, 1) : 1;
        Money basePrice = comboCode != null ? priceSettings.amountFor(comboCode).times(especialMultiplier) : Money.ZERO;

        if (!basePrice.isZero()) {
            if (!hasNonEggPrinciple && eggAdditionals > 0 && additionalsTotal > 0) {
                additionalsTotal = Math.max(0L, additionalsTotal - priceSettings.amountFor(acompananteAdicionCode(mealType)).amount());
            }

            long proteinAdditionals = totalProteins > 1
                    ? (totalProteins - 1) * priceSettings.amountFor(proteinaAdicionCode(mealType)).amount()
                    : 0L;

            // A combo plate includes one principio; a second one (e.g. "principio mixto" —
            // two different principios instead of one principio + one acompañante) is
            // priced the same as an extra accompaniment, whether the combo formed with
            // soup (COMPLETO) or without it (BANDEJA) — it never "acts as" the soup.
            if (principleCreditsFromReal > 1) {
                additionalsTotal += (principleCreditsFromReal - 1) * priceSettings.amountFor(acompananteAdicionCode(mealType)).amount();
            }

            long total = basePrice.amount() + extrasTotal + drinksTotal + proteinAdditionals + additionalsTotal
                    + toGoSurcharge.amount();

            return new PricedOrderItem(
                    comboCode.name(),
                    basePrice,
                    Money.of(drinksTotal),
                    Money.of(proteinAdditionals),
                    Money.of(additionalsTotal),
                    Money.of(extrasTotal),
                    Money.ZERO,
                    toGoSurcharge,
                    Money.of(total));
        }

        long individuals = 0L;
        for (OrderItemSelection s : selections) {
            long qty = s.getQuantity() != null ? s.getQuantity() : 1;
            // Effective, not raw, category — same reasoning as lineTotalFor above: a
            // principio "por sopa" ordered alone prices like a sopa suelta.
            ComboCategory effective = effectiveCategory(s);

            if (effective != ComboCategory.BEBIDA) {
                individuals += unitAmount(effective, s.getProduct(), mealType).amount() * qty;
            }
        }

        return new PricedOrderItem(
                soloLabel(effectiveSoupCredits, totalProteins, sideCredits + principleCredits),
                Money.ZERO,
                Money.of(drinksTotal),
                Money.ZERO,
                Money.ZERO,
                Money.ZERO,
                Money.of(individuals),
                toGoSurcharge,
                Money.of(individuals + drinksTotal + toGoSurcharge.amount()));
    }

    /** What to call a selection that didn't form a priced combo — "solo sopa", "solo
     * proteína", etc. when only one kind of thing was picked, "sueltos" for a mix,
     * or null when there's nothing here worth naming (e.g. just a drink). */
    private String soloLabel(long soupCredits, long totalProteins, long sideAndPrincipleCredits) {
        boolean hasSoup = soupCredits > 0;
        boolean hasProtein = totalProteins > 0;
        boolean hasSide = sideAndPrincipleCredits > 0;
        int kinds = (hasSoup ? 1 : 0) + (hasProtein ? 1 : 0) + (hasSide ? 1 : 0);

        if (kinds == 0) return null;
        if (kinds > 1) return "SUELTOS";
        return hasSoup ? "SOLO_SOPA" : hasProtein ? "SOLO_PROTEINA" : "SOLO_ACOMPANANTE";
    }

    /**
     * The category a selection actually fills: an explicit replacement on the
     * selection wins, then the product's catalog-level fixed replacement policy,
     * then the product's own category.
     */
    private ComboCategory effectiveCategory(OrderItemSelection selection) {
        if (selection.getReplacementCategory() != null) {
            return selection.getReplacementCategory();
        }
        if (selection.getProduct().getActsAsCategory() != null) {
            return selection.getProduct().getActsAsCategory();
        }
        return selection.getProduct().getCategory().getCode();
    }

    private Money unitAmount(ComboCategory category, Product product, String mealType) {
        String normalizedName = normalize(product.getName());
        return switch (category) {
            case SOPA -> priceSettings.amountFor(sopaSueltaCode(mealType));
            case PRINCIPIO, ADICIONAL, ENVASE -> priceSettings.amountFor(acompananteAdicionCode(mealType));
            case PROTEINA -> priceSettings.amountFor(proteinaSueltaCode(mealType));
            case ACOMPANANTE -> priceSettings.amountFor(acompananteAdicionCode(mealType));
            case ESPECIAL -> priceSettings.amountFor(PriceCode.ESPECIAL_COMPONENTE_SUELTO);
            case BEBIDA -> {
                // Substring matching, not an exact-name match — a product named "CocalCola"
                // (an existing typo in the seed data) still matches "coca"+"cola" fine, and
                // it tolerates "Coca Cola" / "Coca-Cola" / etc. without needing the catalog
                // name to be exact. isPersonal distinguishes size (personal vs. the grande/
                // 1.5L/other catch-all); isCocaCola upcharges Coca-Cola specifically at
                // either size (personal or 1.5L) over a generic drink of the same size.
                boolean isPersonal = normalizedName.contains("personal");
                boolean isCocaCola = normalizedName.contains("coca") && normalizedName.contains("cola");

                if (isPersonal && isCocaCola) yield priceSettings.amountFor(PriceCode.BEBIDA_COCA_COLA_PERSONAL);
                if (isPersonal) yield priceSettings.amountFor(PriceCode.BEBIDA_PERSONAL);
                if (isCocaCola) yield priceSettings.amountFor(PriceCode.BEBIDA_COCA_COLA_1_5);
                yield priceSettings.amountFor(PriceCode.BEBIDA_DEFAULT);
            }
        };
    }

    private PriceCode sopaSueltaCode(String mealType) {
        return "ALMUERZO".equalsIgnoreCase(mealType) ? PriceCode.ALMUERZO_SOPA_SUELTA
                : "ESPECIAL".equalsIgnoreCase(mealType) ? PriceCode.ESPECIAL_COMPONENTE_SUELTO
                : PriceCode.DESAYUNO_CALDO_SUELTO;
    }

    private PriceCode proteinaSueltaCode(String mealType) {
        return "ALMUERZO".equalsIgnoreCase(mealType) ? PriceCode.ALMUERZO_PROTEINA_SUELTA
                : "ESPECIAL".equalsIgnoreCase(mealType) ? PriceCode.ESPECIAL_COMPONENTE_SUELTO
                : PriceCode.DESAYUNO_PROTEINA_SUELTA;
    }

    private PriceCode acompananteAdicionCode(String mealType) {
        return "ALMUERZO".equalsIgnoreCase(mealType) ? PriceCode.ALMUERZO_ACOMPANANTE_ADICION
                : "ESPECIAL".equalsIgnoreCase(mealType) ? PriceCode.ESPECIAL_ACOMPANANTE_ADICION
                : PriceCode.DESAYUNO_ACOMPANANTE_ADICION;
    }

    private PriceCode proteinaAdicionCode(String mealType) {
        return "ALMUERZO".equalsIgnoreCase(mealType) ? PriceCode.ALMUERZO_PROTEINA_ADICION
                : "ESPECIAL".equalsIgnoreCase(mealType) ? PriceCode.ESPECIAL_PROTEINA_ADICION
                : PriceCode.DESAYUNO_PROTEINA_ADICION;
    }

    private String extractProteinType(String normalizedProductName) {
        if (normalizedProductName.contains("cerdo")) return "cerdo";
        if (normalizedProductName.contains("pollo")) return "pollo";
        if (normalizedProductName.contains("pescado") || normalizedProductName.contains("mojarra")) return "pescado";
        if (normalizedProductName.contains("res") || normalizedProductName.contains("carne")) return "res";
        if (normalizedProductName.startsWith("huevo")) return "huevo";
        return normalizedProductName;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        n = n.replace('ñ', 'n').replace('Ñ', 'N');
        return n.toLowerCase().trim();
    }
}
