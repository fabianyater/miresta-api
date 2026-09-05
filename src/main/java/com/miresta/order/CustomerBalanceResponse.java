package com.miresta.order;

import com.miresta.shared.Money;

public record CustomerBalanceResponse(Long customerId, String customerName, long pendingOrders, Money totalOwed) {
}
