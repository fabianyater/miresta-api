package com.miresta.services.impl;

import com.miresta.entity.*;
import com.miresta.repository.OderItemRepository;
import com.miresta.repository.OrderTypeRepository;
import com.miresta.services.IOrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class OrderItemServiceImpl implements IOrderItemService {
    private static final Long BASE_FULL_PRICE_LUNCH = 10000L;
    private static final Long BASE_TRAY_PRICE_LUNCH = 9000L;
    private static final Long BASE_FULL_PRICE_BREAKFAST = 8000L;
    private static final Long BASE_TRAY_PRICE_BREAKFAST = 7000L;
    private static final Long TOGO_PRICE = 1000L;
    private static final int SIDES_WITH_SOUP_FULL = 2;
    private static final int SIDES_TRAY = 2;

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
        long soups = 0, principles = 0, proteins = 0, sides = 0, drinks = 0, especials = 0;
        long extrasTotal = 0L;

        for (OrderItemSelection s : selections) {
            long qty = s.getQuantity() != null ? s.getQuantity() : 1;
            long extraPrice = s.getUnitExtraPrice() != null ? s.getUnitExtraPrice() : 0L;

            if (extraPrice > 0) {
                extrasTotal += extraPrice;
            }

            String cat = normalize(s.getProduct().getCategory().getName()); // "SOUP", "PRINCIPLE", "PROTEIN", "SIDE", "ADDITION"

            switch (cat) {
                case "sopa" -> soups += qty;
                case "principios" -> principles += qty;
                case "proteinas" -> proteins += qty;
                case "acompanantes" -> sides += qty;
                case "especiales" -> especials += qty;
                default -> drinks += individualsUnitPrice(cat, s.getProduct().getName()) * qty;

            }
        }

        String mealType = oi.getMenuService().getFoodType().getName(); // "LUNCH" o "BREAKFAST"
        long basePrice = 0L;

        if ("ALMUERZO".equals(mealType)) {
            boolean full = soups >= 1 && principles >= 1 && proteins >= 1 && sides >= SIDES_WITH_SOUP_FULL;
            boolean tray = soups == 0 && principles >= 1 && proteins >= 1 && sides >= SIDES_TRAY;

            basePrice = full ? BASE_FULL_PRICE_LUNCH : (tray ? BASE_TRAY_PRICE_LUNCH : 0L);
        } else if ("DESAYUNO".equals(mealType)) {
            boolean full = soups >= 1 && proteins >= 1 && sides >= 2;
            boolean tray = soups == 0 && proteins >= 1 && sides >= 2;

            basePrice = full ? BASE_FULL_PRICE_BREAKFAST : (tray ? BASE_TRAY_PRICE_BREAKFAST : 0L);
        }

        long toGo = "OUT".equals(oi.getOrderType().getName()) ? TOGO_PRICE : 0L;

        if (basePrice > 0) {
            long total = basePrice + extrasTotal + drinks + toGo;

            oi.setTotal(total);

            orderItemRepository.save(oi);

            return;

        }

        long individuals = 0;

        for (OrderItemSelection s : selections) {
            long qty = s.getQuantity() != null ? s.getQuantity() : 1;
            String cat = normalize(s.getProduct().getCategory().getName());
            String name = normalize(s.getProduct().getName());
            long perUnit = individualsUnitPrice(cat, name);

            individuals += perUnit * qty;
        }

        long total = individuals + extrasTotal + drinks + toGo;
        oi.setTotal(total);

        orderItemRepository.save(oi);
    }

    private long individualsUnitPrice(String category, String productName) {
        switch (category) {
            case "sopa" -> {
                return 5000L;
            }
            case "principios" -> {
                return 1000L;
            }
            case "proteinas" -> {
                if (productName.startsWith("huevo") || productName.startsWith("huevos")) return 4000L;
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
