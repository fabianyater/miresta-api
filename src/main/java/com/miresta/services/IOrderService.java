package com.miresta.services;

import com.miresta.dto.request.CreateOrderRequest;
import com.miresta.dto.response.OrderDetailsResponse;
import com.miresta.dto.response.OrdersResponse;

import java.util.List;

public interface IOrderService {
    void createOrder(CreateOrderRequest orderRequest);
    List<OrdersResponse> getOrders();
    OrderDetailsResponse getOrderDetail(Long orderId);
    OrderDetailsResponse getPendingOrderDetail(Long tableId);
}
