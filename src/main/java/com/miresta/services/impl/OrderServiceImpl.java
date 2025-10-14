package com.miresta.services.impl;

import com.miresta.dto.CreateOrderRequest;
import com.miresta.entity.DiningTable;
import com.miresta.entity.Order;
import com.miresta.entity.OrderItem;
import com.miresta.repository.DiningRepository;
import com.miresta.repository.OrderRepository;
import com.miresta.repository.OrderStatusRepository;
import com.miresta.services.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements IOrderService {
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
            OrderItem orderItem = orderItemService.createOrderItem(savedOrder, o.menuId(), o.mealType(), o.isToGo());
            orderItemSelectionsService.createOrderItemSelection(orderItem, o.items());
        });

    }
}
