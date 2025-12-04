package com.miresta.dto.response;

import java.util.List;

public record OrderItemResponse(
        Long id,
        String comments,
        Long baseTotal,
        Long total,
        MenuServiceResponse menuService,
        OrderTypeDto orderType,
        List<GroupedOrderItemResponse> itemsByCategory) {
}