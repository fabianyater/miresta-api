package com.miresta.order;

import java.util.List;

public record CreateOrderRequest(List<OrderRequest> orders, Long tableId, Long customerId) {
}
