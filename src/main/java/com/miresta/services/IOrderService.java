package com.miresta.services;

import com.miresta.dto.CreateOrderRequest;
import com.miresta.dto.OrderDetailResponse;

import java.util.List;

public interface IOrderService {
    void createOrder(CreateOrderRequest orderRequest);
    List<OrderDetailResponse> getOrders();
    OrderDetailResponse getOrderDetail(Long orderId);
}
