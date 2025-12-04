package com.miresta.dto.response;

import java.util.List;

public record GroupedOrderItemResponse(String category,
                                       List<OrderItemProductResponse> products) {
}
