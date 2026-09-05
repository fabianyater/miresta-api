package com.miresta.order;

import com.miresta.customer.CustomerResponse;
import com.miresta.shared.Money;
import com.miresta.table.DiningTableResponse;

import java.time.Instant;

public record OrdersResponse(
        Long id,
        Instant createdAt,
        String notes,
        Money subtotal,
        Money total,
        DiningTableResponse diningTable,
        OrderStatusDto orderStatus,
        CustomerResponse customer,
        PaymentTypeResponse paymentType,
        boolean paid) {
}
