package com.miresta.order;

import com.miresta.catalog.ProductWithIdAndQuantity;

import java.util.List;

public interface IOrderItemSelectionsService {
    List<OrderItemSelection> getOrderItemSelectionByOrderItem(OrderItem orderItem);

    void createOrderItemSelection(OrderItem orderItem, List<ProductWithIdAndQuantity> items);
}
