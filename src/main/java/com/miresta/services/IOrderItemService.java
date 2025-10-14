package com.miresta.services;

import com.miresta.entity.Order;
import com.miresta.entity.OrderItem;

public interface IOrderItemService {
    OrderItem createOrderItem(Order order, Long menuId, String mealType, Boolean isToGo);
}
