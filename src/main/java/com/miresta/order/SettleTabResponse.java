package com.miresta.order;

import com.miresta.shared.Money;

public record SettleTabResponse(int ordersSettled, Money totalPaid) {
}
