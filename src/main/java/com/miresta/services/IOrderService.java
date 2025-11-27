package com.miresta.services;

import com.miresta.dto.CreateOrderRequest;
import com.miresta.dto.OrderDetailsResponse;
import com.miresta.dto.OrdersResponse;

import java.util.List;

public interface IOrderService {
    void createOrder(CreateOrderRequest orderRequest);
    List<OrdersResponse> getOrders();
    OrderDetailsResponse getOrderDetail(Long orderId);
}
