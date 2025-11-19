package com.miresta.dto;

import java.time.Instant;
import java.util.List;

public record OrderDetailResponse(
        Long id,
        Instant createdAt,
        String notes,
        Long subtotal,
        Long total,
        DiningTableDto diningTable,
        OrderStatusDto orderStatus,
        List<OrderItemDto> orderItems) {
}
