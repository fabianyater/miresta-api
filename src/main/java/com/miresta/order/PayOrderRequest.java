package com.miresta.order;

import java.util.List;

public record PayOrderRequest(List<PaymentLine> payments) {
}
