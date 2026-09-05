package com.miresta.order;

import com.miresta.shared.Money;

public record PaymentTotalResponse(String paymentTypeName, long orderCount, Money total) {
}
