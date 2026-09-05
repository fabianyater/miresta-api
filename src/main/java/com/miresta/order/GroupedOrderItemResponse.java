package com.miresta.order;

import java.util.List;

public record GroupedOrderItemResponse(String category,
                                       List<OrderItemProductResponse> products) {
}
