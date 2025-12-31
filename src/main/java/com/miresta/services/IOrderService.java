package com.miresta.services;

import com.miresta.dto.request.CreateOrderRequest;
import com.miresta.dto.response.OrderDetailsResponse;
import com.miresta.dto.response.OrdersResponse;

import java.util.List;

public interface IOrderService {
    void createOrder(CreateOrderRequest orderRequest);
    List<OrdersResponse> getOrders(String status);
    OrderDetailsResponse getOrderDetail(Long orderId);
    OrderDetailsResponse getPendingOrderDetail(Long tableId);
    void updateOrderStatus(Long orderId, String status);
}
