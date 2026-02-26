package com.miresta.services.impl;

import com.miresta.dto.request.CreateOrderRequest;
import com.miresta.dto.response.*;
import com.miresta.entity.*;
import com.miresta.repository.DiningRepository;
import com.miresta.repository.OrderRepository;
import com.miresta.repository.OrderStatusRepository;
import com.miresta.services.IOrderService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements IOrderService {
    private static final Long BASE_FULL_PRICE_LUNCH = 10000L;
    private static final Long BASE_TRAY_PRICE_LUNCH = 9000L;
    private static final Long BASE_FULL_PRICE_BREAKFAST = 8000L;
    private static final Long BASE_TRAY_PRICE_BREAKFAST = 7000L;
    private static final Long TOGO_PRICE = 1000L;
    private static final int SIDES_WITH_SOUP_FULL = 2;
    private static final int SIDES_TRAY = 2;

    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final DiningRepository diningRepository;
    private final OrderItemServiceImpl orderItemService;
    private final OrderItemSelectionsImpl orderItemSelectionsService;
    private final TableServiceImpl tableService;

    @Transactional
    @Override
    public void createOrder(CreateOrderRequest orderRequest) {
        DiningTable diningTable;

        if (orderRequest.tableId() != null) {
            diningTable = diningRepository.findById(orderRequest.tableId())
                    .orElseThrow(() -> new RuntimeException("Dining table not found: " + orderRequest.tableId()));

            Optional<Order> existingOrder = orderRepository.findByDiningTable_IdAndDiningTable_Status_NameAndOrderStatus_Name(
                    orderRequest.tableId(), "IN_USE", "PENDING");

            Order savedOrder;

            if (existingOrder.isPresent()) {
                savedOrder = existingOrder.get();
            } else {
                tableService.updateTableStatus(diningTable, "IN_USE");

                Order order = new Order();
                order.setCreatedAt(Instant.now());
                order.setDiningTable(diningTable);
                order.setOrderStatus(orderStatusRepository.findByName("PENDING"));

                savedOrder = orderRepository.save(order);
            }

            orderRequest.orders().forEach(o -> {
                int repetitions = (o.count() != null && o.count() > 0) ? o.count() : 1;

                for (int i = 0; i < repetitions; i++) {
                    OrderItem orderItem = orderItemService.createOrderItem(savedOrder, o.menuId(), o.mealType(), o.isToGo(), o.comments());
                    orderItemSelectionsService.createOrderItemSelection(orderItem, o.items());
                    orderItemService.updateTotalPrice(orderItem);
                }
            });

            calculateTotals(savedOrder);
            orderRepository.save(savedOrder);
        } else {
            Order order = new Order();
            order.setCreatedAt(Instant.now());
            order.setDiningTable(null);
            order.setOrderStatus(orderStatusRepository.findByName("PENDING"));

            Order savedOrder = orderRepository.save(order);

            orderRequest.orders().forEach(o -> {
                int repetitions = (o.count() != null && o.count() > 0) ? o.count() : 1;

                for (int i = 0; i < repetitions; i++) {
                    OrderItem orderItem = orderItemService.createOrderItem(savedOrder, o.menuId(), o.mealType(), o.isToGo(), o.comments());
                    orderItemSelectionsService.createOrderItemSelection(orderItem, o.items());
                    orderItemService.updateTotalPrice(orderItem);
                }
            });

            calculateTotals(savedOrder);
            orderRepository.save(savedOrder);
        }
    }

    @Override
    public List<OrdersResponse> getOrders(String status) {
        return orderRepository.findByOrderStatus_Name(status).stream()
                .map(order -> new OrdersResponse(
                        order.getId(),
                        order.getCreatedAt(),
                        order.getNotes(),
                        order.getSubtotal(),
                        order.getTotal(),
                        mapDiningTable(order.getDiningTable()),
                        mapOrderStatus(order.getOrderStatus())
                ))
                .sorted(Comparator.comparing(OrdersResponse::createdAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public OrderDetailsResponse getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada con id: " + orderId));

        return new OrderDetailsResponse(
                order.getId(),
                order.getCreatedAt(),
                order.getNotes(),
                order.getSubtotal(),
                order.getTotal(),
                mapDiningTable(order.getDiningTable()),
                mapOrderStatus(order.getOrderStatus()),
                order.getOrderItems().stream()
                        .sorted(Comparator.comparing(OrderItem::getId).reversed())
                        .map(this::mapOrderItem)
                        .toList()
        );
    }

    @Override
    public OrderDetailsResponse getPendingOrderDetail(Long tableId) {
        Order order = orderRepository.findByDiningTable_IdAndDiningTable_Status_NameAndOrderStatus_Name(
                        tableId, "IN_USE", "PENDING")
                .orElse(null);

        if (order == null) {
            return null;
        }

        return new OrderDetailsResponse(
                order.getId(),
                order.getCreatedAt(),
                order.getNotes(),
                order.getSubtotal(),
                order.getTotal(),
                mapDiningTable(order.getDiningTable()),
                mapOrderStatus(order.getOrderStatus()),
                order.getOrderItems().stream()
                        .map(this::mapOrderItem)
                        .toList()
        );
    }

    @Transactional
    @Override
    public void updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));

        OrderStatus newStatus = orderStatusRepository.findByName(status);
        if (newStatus == null) {
            throw new EntityNotFoundException("Order status not found: " + status);
        }

        order.setOrderStatus(newStatus);

        orderRepository.save(order);

        if ("COMPLETED".equalsIgnoreCase(status)) {
            Optional.ofNullable(order.getDiningTable()).ifPresent(diningTable -> {
                tableService.updateTableStatus(diningTable, "OPEN");
            });
        }
    }

    private DiningTableResponse mapDiningTable(DiningTable diningTable) {
        if (diningTable == null) {
            return null;
        }
        return new DiningTableResponse(
                diningTable.getId(),
                diningTable.getNumber(),
                diningTable.getStatus() != null ? diningTable.getStatus().getName() : null
        );
    }

    private OrderStatusDto mapOrderStatus(OrderStatus orderStatus) {
        return new OrderStatusDto(
                orderStatus.getId(),
                orderStatus.getName()
        );
    }

    private OrderItemResponse mapOrderItem(OrderItem orderItem) {
        var groupedSelections = orderItem.getOrderItemSelections().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        selection -> selection.getProduct().getCategory() != null
                                ? selection.getProduct().getCategory().getName()
                                : "Sin categoría"
                ));

        List<GroupedOrderItemResponse> itemsByCategory = groupedSelections.entrySet().stream()
                .map(entry -> new GroupedOrderItemResponse(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(selection -> new OrderItemProductResponse(
                                        selection.getProduct().getId(),
                                        selection.getProduct().getName(),
                                        selection.getQuantity(),
                                        selection.getUnitExtraPrice(),
                                        Optional.ofNullable(selection.getProduct().getProductDetails())
                                                .map(details -> details.stream()
                                                        .map(ProductDetails::getPrice)
                                                        .filter(Objects::nonNull)
                                                        .reduce(0L, Long::sum))
                                                .orElse(0L),
                                        selection.getReplacementForCategory()
                                ))
                                .toList()
                ))
                .toList();

        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getComments(),
                orderItem.getBaseTotal(),
                orderItem.getIsTogoPrice(),
                orderItem.getTotal(),
                mapMenuService(orderItem.getMenuService()),
                mapOrderType(orderItem.getOrderType()),
                itemsByCategory
        );

    }

    private MenuServiceResponse mapMenuService(MenuService menuService) {
        return new MenuServiceResponse(
                menuService.getId(),
                menuService.getMenu() != null ? menuService.getMenu().getDate() : null,
                menuService.getFoodType() != null ? menuService.getFoodType().getName() : null
        );
    }

    private OrderTypeDto mapOrderType(OrderType orderType) {
        return new OrderTypeDto(
                orderType.getId(),
                orderType.getName()
        );
    }

    private OrderItemSelectionResponse mapOrderItemSelection(OrderItemSelection selection) {
        return new OrderItemSelectionResponse(
                selection.getId(),
                selection.getQuantity(),
                selection.getUnitExtraPrice(),
                selection.getReplacementForCategory(),
                mapProduct(selection.getProduct())
        );
    }

    private ProductResponse mapProduct(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCategory() != null ? () -> product.getCategory().getName() : null
        );
    }

    private void calculateTotals(Order order) {
        List<OrderItem> items = orderItemService.getOrderItemsByOrder(order);

        long orderSubtotal = 0L;

        for (OrderItem oi : items) {
            orderSubtotal += oi.getTotal() != null ? oi.getTotal() : 0L;
        }

        order.setSubtotal(orderSubtotal);
        order.setTotal(orderSubtotal);
    }
}
