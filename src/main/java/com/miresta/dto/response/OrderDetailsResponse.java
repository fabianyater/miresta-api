package com.miresta.dto.response;

import java.time.Instant;
import java.util.List;

public record OrderDetailsResponse(
        Long id,
        Instant createdAt,
        String notes,
        Long subtotal,
        Long total,
        DiningTableResponse diningTable,
        OrderStatusDto orderStatus,
        List<OrderItemResponse> orderItems) {
}
