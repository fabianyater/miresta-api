package com.miresta.services;

import com.miresta.dto.ProductWIthIdAndQuantity;
import com.miresta.entity.OrderItem;
import com.miresta.entity.OrderItemSelection;

import java.util.List;

public interface IOrderItemSelectionsService {
    List<OrderItemSelection> getOrderItemSelectionByOrderItem(OrderItem orderItem);
    void createOrderItemSelection(OrderItem orderItem, List<ProductWIthIdAndQuantity> item);
}
