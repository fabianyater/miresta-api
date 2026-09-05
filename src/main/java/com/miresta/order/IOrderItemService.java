package com.miresta.order;

import com.miresta.customer.Customer;

import java.util.List;

public interface IOrderItemService {
    OrderItem createOrderItem(Order order, Long menuId, String mealType, Boolean isToGo, String comments, Customer customer);

    List<OrderItem> getOrderItemsByOrder(Order order);

    void updateTotalPrice(OrderItem orderItem);
}
