package com.miresta.dto;

import java.util.List;

public record OrderItemDto(
        Long id,
        String comments,
        Long total,
        MenuServiceDto menuService,
        OrderTypeDto orderType,
        List<GroupedOrderItemDto> itemsByCategory) {
}