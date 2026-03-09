package com.miresta.services.impl;

import com.miresta.entity.*;
import com.miresta.exception.ResourceNotFoundException;
import com.miresta.repository.OrderItemRepository;
import com.miresta.repository.OrderTypeRepository;
import com.miresta.services.IMenuServicesService;
import com.miresta.services.IOrderItemService;
import com.miresta.services.IOrderItemSelectionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
@Slf4j
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

    private final OrderItemRepository orderItemRepository;
    private final IMenuServicesService menuServicesService;
    private final OrderTypeRepository orderTypeRepository;
    private final IOrderItemSelectionsService orderItemSelectionsService;

    // ====== API principal ======

    @Override
    public OrderItem createOrderItem(Order order, Long menuId, String mealType, Boolean isToGo, String comments) {
        String getOrderTypeName = Boolean.TRUE.equals(isToGo) ? "OUT" : "IN";
        OrderType orderType = orderTypeRepository.findByName(getOrderTypeName)
                .orElseThrow(() -> new ResourceNotFoundException("Order type not found: " + getOrderTypeName));

        MenuService menuService = menuServicesService.getMenuServiceByMenuIdAndFoodTypeName(menuId, mealType);
        OrderItem orderItem = new OrderItem();

        orderItem.setOrder(order);
        orderItem.setMenuService(menuService);
        orderItem.setOrderType(orderType);
        orderItem.setComments(comments);

        return orderItemRepository.save(orderItem);
    }

    @Override
    public List<OrderItem> getOrderItemsByOrder(Order order) {
        return orderItemRepository.findAllByOrder(order);
    }

    @Override
    public void updateTotalPrice(OrderItem oi) {
        List<OrderItemSelection> selections = orderItemSelectionsService.getOrderItemSelectionByOrderItem(oi);

        long soupCredits = 0, principleCredits = 0, sideCredits = 0;
        Map<String, Long> proteinsByType = new HashMap<>();

        long extrasTotal = 0L;
        long drinksTotal = 0L;
        long additionalsTotal = 0L;

        boolean hasNonEggPrinciple = false;
        long eggAdditionals = 0L;

        long soupCreditsFromRealSoup = 0L;
        long soupCreditsFromReplacement = 0L;

        for (OrderItemSelection s : selections) {
            long qty = s.getQuantity() != null ? s.getQuantity() : 1;
            long extraPrice = s.getUnitExtraPrice() != null ? s.getUnitExtraPrice() : 0L;

            String cat  = normalize(s.getProduct().getCategory().getName());
            String prod = normalize(s.getProduct().getName());
            boolean isEgg = prod.startsWith("huevo");

            // 1) Resolver replacement ANTES
            ComboCat rep = resolveReplacement(s);

            // 2) SUMAR extraPrice SOLO si aplica (nunca para proteínas/acts-as-proteínas)
            //    OJO: si tu front estuviera enviando unit_extra_price por unidad, cambia a (extraPrice * qty).
            boolean isProteinLine = "proteinas".equals(cat) || rep == ComboCat.PROTEINAS;
            if (extraPrice > 0 && !isProteinLine) {
                extrasTotal += extraPrice;
            }

            // 3) Créditos + cobros por categoría
            switch (cat) {
                case "sopa" -> {
                    soupCredits += qty;
                    soupCreditsFromRealSoup += qty;
                }
                case "principios" -> {
                    principleCredits += qty;
                    if (!isEgg) hasNonEggPrinciple = true;
                }
                case "acompanantes" -> {
                    // Solo ensalada y arroz se cuentan una vez (máximo 1 crédito)
                    if (!prod.contains("ensalada") && !prod.contains("arroz")) {
                        sideCredits += 1; // Siempre suma 1, independientemente de qty
                    } else {
                        sideCredits += qty; // Otros acompañantes se acumulan normalmente
                    }
                    
                    if (isEgg) {
                        proteinsByType.put("huevo", proteinsByType.getOrDefault("huevo", 0L) + qty);
                    }
                }
                case "proteinas" -> {
                    String pType = extractProteinType(prod);
                    proteinsByType.put(pType, proteinsByType.getOrDefault(pType, 0L) + qty);
                }
                case "adicionales" -> {
                    // Si este adicional actúa como PROTEÍNA, NO lo cobres como adicional
                    if (rep == ComboCat.PROTEINAS) {
                        // nada: se contará abajo como proteína por replacement
                    } else {
                        // Cobro standard de adicionales
                        additionalsTotal += 1_000L * qty;
                        if (isEgg) eggAdditionals += qty; // para exención si actúa como PRINCIPIO
                    }
                }
                case "bebidas" -> {
                    long pricePerUnit = individualsUnitPrice(cat, prod);
                    drinksTotal += pricePerUnit * qty;
                }
                case "especiales" -> {
                    // individuales si no califica combo
                }
                default -> {}
            }

            // 4) Créditos por replacement
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
            // Exención: primer huevo usado como PRINCIPIO (si no hubo principio no-huevo)
            if (!hasNonEggPrinciple && eggAdditionals > 0 && additionalsTotal > 0) {
                additionalsTotal = Math.max(0L, additionalsTotal - 1_000L);
            }

            long proteinAdditionals = totalProteins > 1 ? calculateProteinAdditionals(proteinsByType) : 0L;

            long total = basePrice + extrasTotal + drinksTotal + toGo + proteinAdditionals + additionalsTotal;
            oi.setTotal(total);
            oi.setIsTogoPrice(toGo);
            oi.setBaseTotal(basePrice);
            orderItemRepository.save(oi);
            return;
        }

        // No combo: individuales
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
        oi.setIsTogoPrice(toGo);
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
                if (!productName.contains("ensalada") && !productName.contains("arroz")) return 0L;
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

    // ====== Reemplazos ======

    private enum ComboCat { SOPA, PRINCIPIOS, PROTEINAS, ACOMPANANTES }

    /**
     * Resuelve el reemplazo declarado:
     * 1) OrderItemSelection.getReplacementForCategory() -> "sopa|principios|proteinas|acompanantes"
     * 2) Product.getActsAsCategory() (para políticas fijas en catálogo)
     */
    private ComboCat resolveReplacement(OrderItemSelection s) {
        ComboCat parsed = parseComboCat(s.getReplacementForCategory());
        if (parsed != null) return parsed;
        return parseComboCat(s.getProduct().getActsAsCategory());
    }

    private ComboCat parseComboCat(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = normalize(raw);
        return switch (v) {
            case "sopa"         -> ComboCat.SOPA;
            case "principios"   -> ComboCat.PRINCIPIOS;
            case "proteinas"    -> ComboCat.PROTEINAS;
            case "acompanantes" -> ComboCat.ACOMPANANTES;
            default             -> null;
        };
    }
}
