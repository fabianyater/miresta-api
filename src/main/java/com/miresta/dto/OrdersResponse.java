package com.miresta.dto;

import java.time.Instant;

public record OrdersResponse(
        Long id,
        Instant createdAt,
        String notes,
        Long subtotal,
        Long total,
        DiningTableDto diningTable,
        OrderStatusDto orderStatus) {
}
