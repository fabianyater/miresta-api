package com.miresta.dto.request;

import java.util.List;

public record CreateOrderRequest(List<OrderRequest> orders, Long tableId) {
}
