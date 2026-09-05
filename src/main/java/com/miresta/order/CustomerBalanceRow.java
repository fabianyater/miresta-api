package com.miresta.order;

public record CustomerBalanceRow(Long customerId, String customerName, long pendingOrders, Long totalOwed) {
}
