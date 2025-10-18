package com.miresta.services.impl;

import com.miresta.dto.CreateOrderRequest;
import com.miresta.entity.DiningTable;
import com.miresta.entity.Order;
import com.miresta.entity.OrderItem;
import com.miresta.entity.OrderItemSelection;
import com.miresta.repository.DiningRepository;
import com.miresta.repository.OrderRepository;
import com.miresta.repository.OrderStatusRepository;
import com.miresta.services.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

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
        DiningTable diningTable = diningRepository.findById(orderRequest.tableId())
                .orElseThrow(() -> new RuntimeException("Dining table not found: " + orderRequest.tableId()));
        tableService.updateTableStatus(diningTable, "IN_USE");

        Order order = new Order();

        order.setCreatedAt(Instant.now());
        order.setDiningTable(diningTable);
        order.setOrderStatus(orderStatusRepository.findByName("PENDING"));

        Order savedOrder = orderRepository.save(order);


        orderRequest.orders().forEach(o -> {
            OrderItem orderItem = orderItemService.createOrderItem(savedOrder, o.menuId(), o.mealType(), o.isToGo(), o.comments());
            orderItemSelectionsService.createOrderItemSelection(orderItem, o.items());
            orderItemService.updateTotalPrice(orderItem);

        });

        calculateTotals(savedOrder);
        orderRepository.save(savedOrder);
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
