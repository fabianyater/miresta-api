package com.miresta.services;

import com.miresta.entity.Order;
import com.miresta.entity.OrderItem;

import java.util.List;

public interface IOrderItemService {
    OrderItem createOrderItem(Order order, Long menuId, String mealType, Boolean isToGo);
    List<OrderItem> getOrderItemsByOrder(Order order);
    void updateTotalPrice(OrderItem orderItem);
}
