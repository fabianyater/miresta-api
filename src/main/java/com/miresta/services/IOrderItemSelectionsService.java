package com.miresta.services;

import com.miresta.dto.ProductWIthIdAndQuantity;
import com.miresta.entity.OrderItem;

import java.util.List;

public interface IOrderItemSelectionsService {
    void createOrderItemSelection(OrderItem orderItem, List<ProductWIthIdAndQuantity> item);
}
