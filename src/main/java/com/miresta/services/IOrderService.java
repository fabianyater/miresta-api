package com.miresta.services;

import com.miresta.dto.CreateOrderRequest;

public interface IOrderService {
    void createOrder(CreateOrderRequest orderRequest);
}
