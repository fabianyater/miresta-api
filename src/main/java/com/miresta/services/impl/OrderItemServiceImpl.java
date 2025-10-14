package com.miresta.services.impl;

import com.miresta.entity.MenuService;
import com.miresta.entity.Order;
import com.miresta.entity.OrderItem;
import com.miresta.entity.OrderType;
import com.miresta.repository.OderItemRepository;
import com.miresta.repository.OrderTypeRepository;
import com.miresta.services.IOrderItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class OrderItemServiceImpl implements IOrderItemService {
    private final OderItemRepository orderItemRepository;
    private final MenuServicesServiceImpl menuServicesService;
    private final OrderTypeRepository orderTypeRepository;

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
}
