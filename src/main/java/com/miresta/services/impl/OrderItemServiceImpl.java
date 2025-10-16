package com.miresta.services.impl;

import com.miresta.entity.*;
import com.miresta.repository.OderItemRepository;
import com.miresta.repository.OrderTypeRepository;
import com.miresta.services.IOrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RequiredArgsConstructor
@Service
public class OrderItemServiceImpl implements IOrderItemService {
    private static final Long BASE_FULL_PRICE_LUNCH = 10000L;
    private static final Long BASE_TRAY_PRICE_LUNCH = 9000L;
    private static final Long BASE_FULL_PRICE_BREAKFAST = 8000L;
    private static final Long BASE_TRAY_PRICE_BREAKFAST = 7000L;
    private static final Long TOGO_PRICE = 1000L;
    private static final int SIDES = 2;

    private final OderItemRepository orderItemRepository;
    private final MenuServicesServiceImpl menuServicesService;
    private final OrderTypeRepository orderTypeRepository;
    private final OrderItemSelectionsImpl orderItemSelectionsService;

    @Override
    public OrderItem createOrderItem(Order order, Long menuId, String mealType, Boolean isToGo) {
        String getOrderTypeName = isToGo ? "OUT" : "IN";
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

        long soups = 0, principles = 0, sides = 0, drinks = 0, additionals = 0, especials = 0;
        long extrasTotal = 0L;

        // Proteínas por tipo
        Map<String, Long> proteinsByType = new HashMap<>();

        // Banderas/contadores específicos de huevo por categoría
        boolean hasEggsAsPrinciple = false;   // huevo en "principios"
        boolean hasEggsInSoup = false;        // huevo en "sopa"
        boolean hasEggsInSides = false;       // huevo en "acompañantes"
        boolean hasEggsAsProtein = false;     // huevo en "proteínas"
        boolean hasEggsAsAdditional = false;  // huevo en "adicionales"

        long drinksTotal = 0L;
        long additionalsTotal = 0L;

        // NUEVOS contadores para aplicar exención del primer huevo cuando reemplaza "principio"
        long eggPrinciples = 0L;        // huevos que llegaron por "principios"
        long eggAdditionals = 0L;       // huevos que llegaron por "adicionales"
        boolean hasNonEggPrinciple = false; // existe un principio que no es huevo
        long nonEggPrinciples = 0L;

        for (OrderItemSelection s : selections) {
            long qty = s.getQuantity() != null ? s.getQuantity() : 1;
            long extraPrice = s.getUnitExtraPrice() != null ? s.getUnitExtraPrice() : 0L;

            if (extraPrice > 0) {
                extrasTotal += extraPrice;
            }

            String cat = normalize(s.getProduct().getCategory().getName());
            String productName = normalize(s.getProduct().getName());

            boolean isEgg = productName.startsWith("huevo");

            switch (cat) {
                case "sopa" -> {
                    if (isEgg) {
                        hasEggsInSoup = true;
                        // La sopa con huevo NO cuenta como sopa para el combo
                    } else {
                        soups += qty;
                    }
                }
                case "principios" -> {
                    if (isEgg) {
                        hasEggsAsPrinciple = true;
                        eggPrinciples += qty;
                        principles += qty; // tu lógica original: cuenta principio aunque sea huevo
                    } else {
                        hasNonEggPrinciple = true;
                        nonEggPrinciples += qty;
                        principles += qty;
                    }
                }
                case "proteinas" -> {
                    String proteinType = extractProteinType(productName);
                    proteinsByType.put(proteinType, proteinsByType.getOrDefault(proteinType, 0L) + qty);

                    if (isEgg) {
                        hasEggsAsProtein = true;
                    }
                }
                case "acompanantes" -> {
                    if (isEgg) {
                        hasEggsInSides = true;
                        sides += qty;
                        // Mantienes tu regla: huevo en acompañante también cuenta como proteína
                        proteinsByType.put("huevo", proteinsByType.getOrDefault("huevo", 0L) + qty);
                    } else {
                        sides += qty;
                    }
                }
                case "especiales" -> especials += qty;
                case "adicionales" -> {
                    if (isEgg) {
                        hasEggsAsAdditional = true;
                        eggAdditionals += qty;
                        additionals += qty;
                        // Cobro estándar de adicionales (luego haremos exención del primero si aplica)
                        additionalsTotal += 1000L * qty;
                    } else {
                        additionals += qty;
                        additionalsTotal += 1000L * qty;
                    }
                }
                case "bebidas" -> {
                    long pricePerUnit = individualsUnitPrice(cat, productName);
                    drinksTotal += pricePerUnit * qty;
                }
                default -> {}
            }
        }

        long totalProteins = proteinsByType.values().stream().mapToLong(Long::longValue).sum();

        String mealType = oi.getMenuService().getFoodType().getName();
        long basePrice = 0L;

        if ("ALMUERZO".equalsIgnoreCase(mealType)) {
            boolean hasBasicCombo = totalProteins >= 1 && sides >= SIDES;

            if (hasEggsInSoup) {
                // Con huevo en sopa no cuenta sopa -> posible bandeja
                boolean tray = hasBasicCombo;
                basePrice = tray ? BASE_TRAY_PRICE_LUNCH : 0L;
            } else {
                // full = sopa + (proteína >=1) + (acompañantes >=2)
                boolean full = soups >= 1 && hasBasicCombo;
                boolean tray = soups == 0 && hasBasicCombo;
                basePrice = full ? BASE_FULL_PRICE_LUNCH : (tray ? BASE_TRAY_PRICE_LUNCH : 0L);
            }
        } else if ("DESAYUNO".equalsIgnoreCase(mealType)) {
            boolean full = soups >= 1 && totalProteins >= 1 && sides >= 2;
            boolean tray = soups == 0 && totalProteins >= 1 && sides >= 2;
            basePrice = full ? BASE_FULL_PRICE_BREAKFAST : (tray ? BASE_TRAY_PRICE_BREAKFAST : 0L);
        }

        long toGo = "OUT".equals(oi.getOrderType().getName()) ? TOGO_PRICE : 0L;

        if (basePrice > 0) {
            // >>> EXENCIÓN: si NO hubo principio no-huevo, el principio lo aporta un huevo.
            // Si ese huevo vino como ADICIONAL, el PRIMERO debe ser gratis dentro del combo/bandeja.
            if (!hasNonEggPrinciple) {
                if (eggAdditionals > 0 && additionalsTotal > 0) {
                    additionalsTotal -= 1000L; // exención del primer huevo usado como principio
                    if (additionalsTotal < 0) additionalsTotal = 0; // seguridad
                }
                // Si el huevo vino por "principios", no se cobró aparte, así que nada que descontar.
            }

            long proteinAdditionals = totalProteins > 1 ? calculateProteinAdditionals(proteinsByType) : 0L;

            long total = basePrice + extrasTotal + drinksTotal + toGo + proteinAdditionals + additionalsTotal;

            oi.setTotal(total);
            orderItemRepository.save(oi);
            return;
        }

        // Si no califica como combo/bandeja, cobrar individual
        long individuals = 0L;
        for (OrderItemSelection s : selections) {
            long qty = s.getQuantity() != null ? s.getQuantity() : 1;
            String cat = normalize(s.getProduct().getCategory().getName());
            String name = normalize(s.getProduct().getName());

            if (!cat.equals("bebidas")) { // bebidas ya contabilizadas
                long perUnit = individualsUnitPrice(cat, name);
                individuals += perUnit * qty;
            }
        }

        long total = individuals + drinksTotal + toGo;
        oi.setTotal(total);
        orderItemRepository.save(oi);
    }

    /**
     * Calcula el costo adicional cuando hay múltiples proteínas de diferentes tipos.
     * La primera proteína está incluida en el base price, las adicionales cuestan 4000 cada una.
     */
    private long calculateProteinAdditionals(Map<String, Long> proteinsByType) {
        if (proteinsByType.isEmpty()) return 0L;

        long totalProteinCount = 0L;
        for (Long count : proteinsByType.values()) {
            totalProteinCount += count;
        }
        if (totalProteinCount <= 1) return 0L;

        long additionalProteins = totalProteinCount - 1;
        return additionalProteins * 4000L;
    }

    /**
     * Extrae el tipo de proteína del nombre del producto
     */
    private String extractProteinType(String productName) {
        if (productName.contains("cerdo")) return "cerdo";
        if (productName.contains("pollo")) return "pollo";
        if (productName.contains("pescado") || productName.contains("mojarra")) return "pescado";
        if (productName.contains("res") || productName.contains("carne")) return "res";
        if (productName.startsWith("huevo")) return "huevo";
        return productName;
    }

    private long individualsUnitPrice(String category, String productName) {
        switch (category) {
            case "sopa" -> {
                return 5000L;
            }
            case "principios", "adicionales" -> {
                return 1000L;
            }
            case "proteinas" -> {
                if (productName.startsWith("huevo")) return 4000L;
                return 4000L;
            }
            case "acompanantes" -> {
                if (productName.contains("maduro")) return 0L;
                return 1000L;
            }
            case "especiales" -> {
                return 10000L;
            }
            case "bebidas" -> {
                if (productName.equals("coca-cola-1.5")) return 7000L;
                if (productName.contains("personal")) return 3000L;
                return 6000L;
            }
            default -> {
                return 0L;
            }
        }
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        n = n.replace('ñ', 'n').replace('Ñ', 'N');
        return n.toLowerCase().trim();
    }
}
