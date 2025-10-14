package com.miresta.dto;

import com.miresta.entity.OrderItem;

import java.util.List;

public record CreateOrderRequest(List<OrderRequest> orders, Long tableId) {
}
