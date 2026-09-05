package com.miresta.order;

public record DaySummaryRow(long totalOrders, long cancelledOrders, Long totalSales, long registeredCustomers) {
}
