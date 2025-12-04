package com.miresta.dto.response;

import java.time.Instant;

public record OrdersResponse(
        Long id,
        Instant createdAt,
        String notes,
        Long subtotal,
        Long total,
        DiningTableResponse diningTable,
        OrderStatusDto orderStatus) {
}
