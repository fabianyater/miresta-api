package com.miresta.services.impl;

import com.miresta.entity.*;
import com.miresta.repository.OderItemRepository;
import com.miresta.repository.OrderTypeRepository;
import com.miresta.services.IOrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class OrderItemServiceImpl implements IOrderItemService {

    // ====== Precios base y reglas ======
    private static final long BASE_FULL_PRICE_LUNCH      = 10_000L;
    private static final long BASE_TRAY_PRICE_LUNCH      = 9_000L;
    private static final long BASE_FULL_PRICE_BREAKFAST  = 8_000L;
    private static final long BASE_TRAY_PRICE_BREAKFAST  = 7_000L;
    private static final long TOGO_PRICE                 = 1_000L;
    private static final int  SIDES                      = 2;

    // Política: ¿un “X por sopa” cuenta como sopa real (FULL) o como bandeja?
    // Recomendado: principio/arroz/huevo POR SOPA => Bandeja (false).
    private static final boolean REPLACEMENT_COUNTS_AS_REAL_SOUP = false;

    private final OderItemRepository orderItemRepository;
    private final MenuServicesServiceImpl menuServicesService;
    private final OrderTypeRepository orderTypeRepository;
    private final OrderItemSelectionsImpl orderItemSelectionsService;

    // ====== API principal ======

    @Override
    public OrderItem createOrderItem(Order order, Long menuId, String mealType, Boolean isToGo) {
        String getOrderTypeName = Boolean.TRUE.equals(isToGo) ? "OUT" : "IN";
        OrderType orderType = orderTypeRepository.findByName(getOrderTypeName)
                .orElseThrow(() -> new RuntimeException("Order type not found: " + getOrderTypeName));

        MenuService menuService = menuServicesService.getMenuServiceByMenuIdAndFoodTypeName(menuId, mealType);
        OrderItem orderItem = new OrderItem();

        orderItem.setOrder(order);
        orderItem.setMenuService(menuService);
        orderItem.setOrderType(orderType);

        return orderItemRepository.save(orderItem);
    }

    @Override
    public List<OrderItem> getOrderItemsByOrder(Order order) {
        return orderItemRepository.findAllByOrder(order);
    }

    @Override
    public void updateTotalPrice(OrderItem oi) {
        List<OrderItemSelection> selections = orderItemSelectionsService.getOrderItemSelectionByOrderItem(oi);

        // Créditos para calificar combo
        long soupCredits = 0, principleCredits = 0, sideCredits = 0;
        Map<String, Long> proteinsByType = new HashMap<>();

        // Acumulados monetarios
        long extrasTotal = 0L;
        long drinksTotal = 0L;
        long additionalsTotal = 0L;

        // Bandera para exención del primer huevo cuando reemplaza principio
        boolean hasNonEggPrinciple = false;
        long eggAdditionals = 0L;

        // Para distinguir "sopa real" (categoría Sopa) de "sopa por reemplazo"
        long soupCreditsFromRealSoup = 0L;
        long soupCreditsFromReplacement = 0L;

        for (OrderItemSelection s : selections) {
            long qty = s.getQuantity() != null ? s.getQuantity() : 1;
            long extraPrice = s.getUnitExtraPrice() != null ? s.getUnitExtraPrice() : 0L;
            if (extraPrice > 0) extrasTotal += extraPrice;

            String cat  = normalize(s.getProduct().getCategory().getName());
            String prod = normalize(s.getProduct().getName());
            boolean isEgg = prod.startsWith("huevo");

            // ⚠️ Calcular el replacement ANTES del switch de categoría
            ComboCat rep = resolveReplacement(s);

            // 1) Créditos base por categoría + cobros unitarios
            switch (cat) {
                case "sopa" -> {
                    // Si quieres que "huevo en sopa" NO cuente como sopa real,
                    // podrías condicionar aquí con isEgg. Por defecto, cuenta.
                    soupCredits += qty;
                    soupCreditsFromRealSoup += qty;
                }
                case "principios" -> {
                    principleCredits += qty;
                    if (!isEgg) hasNonEggPrinciple = true;
                }
                case "acompanantes" -> {
                    sideCredits += qty;
                    // Regla existente: huevo en acompañante también suma proteína
                    if (isEgg) {
                        proteinsByType.put("huevo", proteinsByType.getOrDefault("huevo", 0L) + qty);
                    }
                }
                case "proteinas" -> {
                    String pType = extractProteinType(prod);
                    proteinsByType.put(pType, proteinsByType.getOrDefault(pType, 0L) + qty);
                }
                case "adicionales" -> {
                    // --- AJUSTE CLAVE ---
                    // Si este adicional viene marcado como "proteína", NO se cobra como adicional.
                    if (rep == ComboCat.PROTEINAS) {
                        // No sumar additionalsTotal; se contará como proteína en el bloque de replacements.
                        // (Opcional: si quieres que "huevo por sopa/acompanante" tampoco cobre adicional, añade aquí rep==SOPA/ACOMPANANTES).
                    } else {
                        // Cobro standard adicional (1.000 c/u)
                        additionalsTotal += 1_000L * qty;
                        if (isEgg) eggAdditionals += qty; // Necesario para exención del 1er huevo como PRINCIPIO
                    }
                }
                case "bebidas" -> {
                    long pricePerUnit = individualsUnitPrice(cat, prod);
                    drinksTotal += pricePerUnit * qty;
                }
                case "especiales" -> {
                    // Se cobrará en individuales si no califica combo
                }
                default -> {}
            }

            // 2) Aplicar créditos por reemplazos declarados
            if (rep != null) {
                switch (rep) {
                    case SOPA -> {
                        soupCredits += qty;
                        soupCreditsFromReplacement += qty;
                    }
                    case PRINCIPIOS -> principleCredits += qty;
                    case PROTEINAS -> {
                        String pType = extractProteinType(prod);
                        proteinsByType.put(pType, proteinsByType.getOrDefault(pType, 0L) + qty);
                    }
                    case ACOMPANANTES -> sideCredits += qty;
                }
            }
        }

        long totalProteins = proteinsByType.values().stream().mapToLong(Long::longValue).sum();

        // 3) Calcular base price (full / bandeja / ninguno)
        String mealType = oi.getMenuService().getFoodType().getName();
        long basePrice = 0L;

        long effectiveSoupCredits = soupCreditsFromRealSoup
                + (REPLACEMENT_COUNTS_AS_REAL_SOUP ? soupCreditsFromReplacement : 0);

        if ("ALMUERZO".equalsIgnoreCase(mealType)) {
            boolean hasBasicCombo = totalProteins >= 1 && sideCredits >= SIDES;
            boolean full = (effectiveSoupCredits >= 1) && hasBasicCombo;
             boolean tray = (effectiveSoupCredits == 0) && hasBasicCombo;

            basePrice = full ? BASE_FULL_PRICE_LUNCH : (tray ? BASE_TRAY_PRICE_LUNCH : 0L);
        } else if ("DESAYUNO".equalsIgnoreCase(mealType)) {
            boolean full = (effectiveSoupCredits >= 1) && totalProteins >= 1 && sideCredits >= 2;
            boolean tray = (effectiveSoupCredits == 0) && totalProteins >= 1 && sideCredits >= 2;
            basePrice = full ? BASE_FULL_PRICE_BREAKFAST : (tray ? BASE_TRAY_PRICE_BREAKFAST : 0L);
        }

        long toGo = "OUT".equals(oi.getOrderType().getName()) ? TOGO_PRICE : 0L;

        if (basePrice > 0) {
            // 4) Exención del 1er huevo cuando reemplaza PRINCIPIO y no hubo principio no-huevo
            if (!hasNonEggPrinciple && eggAdditionals > 0 && additionalsTotal > 0) {
                additionalsTotal = Math.max(0L, additionalsTotal - 1_000L);
            }

            // 5) Proteínas adicionales
            long proteinAdditionals = totalProteins > 1 ? calculateProteinAdditionals(proteinsByType) : 0L;

            long total = basePrice + extrasTotal + drinksTotal + toGo + proteinAdditionals + additionalsTotal;
            oi.setTotal(total);
            orderItemRepository.save(oi);
            return;
        }

        // 6) Si no calificó combo/bandeja: cobro individual
        long individuals = 0L;
        for (OrderItemSelection s : selections) {
            long qty = s.getQuantity() != null ? s.getQuantity() : 1;
            String cat = normalize(s.getProduct().getCategory().getName());
            String name = normalize(s.getProduct().getName());

            if (!"bebidas".equals(cat)) {
                long perUnit = individualsUnitPrice(cat, name);
                individuals += perUnit * qty;
            }
        }

        long total = individuals + drinksTotal + toGo;
        oi.setTotal(total);
        orderItemRepository.save(oi);
    }

    // ====== Helpers de negocio ======

    /**
     * Proteína incluida: 1. A partir de la 2.ª, +4.000 c/u
     */
    private long calculateProteinAdditionals(Map<String, Long> proteinsByType) {
        if (proteinsByType == null || proteinsByType.isEmpty()) return 0L;
        long totalProteinCount = 0L;
        for (Long count : proteinsByType.values()) totalProteinCount += (count != null ? count : 0L);
        if (totalProteinCount <= 1) return 0L;
        return (totalProteinCount - 1) * 4_000L;
    }

    /**
     * Extrae tipo de proteína desde el nombre normalizado del producto.
     */
    private String extractProteinType(String productName) {
        if (productName.contains("cerdo")) return "cerdo";
        if (productName.contains("pollo")) return "pollo";
        if (productName.contains("pescado") || productName.contains("mojarra")) return "pescado";
        if (productName.contains("res") || productName.contains("carne")) return "res";
        if (productName.startsWith("huevo")) return "huevo";
        return productName; // fallback
    }

    /**
     * Precios individuales por categoría (tu misma tabla).
     */
    private long individualsUnitPrice(String category, String productName) {
        switch (category) {
            case "sopa" -> {
                return 5_000L;
            }
            case "principios", "adicionales" -> {
                return 1_000L;
            }
            case "proteinas" -> {
                return 4_000L;
            }
            case "acompanantes" -> {
                if (productName.contains("maduro")) return 0L;
                return 1_000L;
            }
            case "especiales" -> {
                return 10_000L;
            }
            case "bebidas" -> {
                if ("coca-cola-1.5".equals(productName)) return 7_000L;
                if (productName.contains("personal")) return 3_000L;
                return 6_000L;
            }
            default -> {
                return 0L;
            }
        }
    }

    /**
     * Normaliza strings: sin tildes, minúsculas, sin espacios extremos.
     */
    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        n = n.replace('ñ', 'n').replace('Ñ', 'N');
        return n.toLowerCase().trim();
    }

    // ====== Reemplazos (reflection-safe) ======

    private enum ComboCat { SOPA, PRINCIPIOS, PROTEINAS, ACOMPANANTES }

    /**
     * Intenta resolver un reemplazo declarado:
     * 1) OrderItemSelection.getReplacementForCategory() -> "sopa|principios|proteinas|acompanantes"
     * 2) Product.getActsAsCategory() (para políticas fijas en catálogo)
     * Si no existe el método (no has migrado aún), no pasa nada y devuelve null.
     */
    private ComboCat resolveReplacement(OrderItemSelection s) {
        // 1) Replacement a nivel selección (UI)
        String rep = getReplacementForCategorySafe(s);
        if (rep != null) {
            ComboCat parsed = parseComboCat(rep);
            if (parsed != null) return parsed;
        }

        // 2) Replacement a nivel catálogo (producto)
        String actsAs = getActsAsCategorySafe(s.getProduct());
        if (actsAs != null) {
            ComboCat parsed = parseComboCat(actsAs);
            if (parsed != null) return parsed;
        }

        // 3) Sin reemplazo
        return null;
    }

    private ComboCat parseComboCat(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = normalize(raw);
        switch (v) {
            case "sopa":          return ComboCat.SOPA;
            case "principios":    return ComboCat.PRINCIPIOS;
            case "proteinas":     return ComboCat.PROTEINAS;
            case "acompanantes":  return ComboCat.ACOMPANANTES;
            default:              return null;
        }
    }

    // Reflection para no romper si aún no agregas el campo
    private String getReplacementForCategorySafe(OrderItemSelection s) {
        try {
            Method m = s.getClass().getMethod("getReplacementForCategory");
            Object val = m.invoke(s);
            return val != null ? String.valueOf(val) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    // Reflection para política fija en catálogo
    private String getActsAsCategorySafe(Product p) {
        try {
            Method m = p.getClass().getMethod("getActsAsCategory");
            Object val = m.invoke(p);
            return val != null ? String.valueOf(val) : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
