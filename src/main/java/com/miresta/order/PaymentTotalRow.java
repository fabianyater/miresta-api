package com.miresta.order;

public record PaymentTotalRow(String paymentTypeName, long orderCount, Long total) {
}
