package com.miresta.dto;

import java.util.List;

public record GroupedOrderItemDto(String category,
                                  List<OrderItemProductDto> products) {
}
