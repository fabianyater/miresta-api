package com.miresta.order;

import com.miresta.customer.CustomerResponse;
import com.miresta.shared.Money;
import com.miresta.table.DiningTableResponse;

import java.time.Instant;
import java.util.List;

public record OrdersResponse(
        Long id,
        Instant createdAt,
        String notes,
        Money subtotal,
        Money total,
        DiningTableResponse diningTable,
        OrderStatusDto orderStatus,
        CustomerResponse customer,
        // Solo se llena cuando se pagó con un único método — con pagos divididos queda
        // null y el desglose real vive en `payments`.
        PaymentTypeResponse paymentType,
        boolean paid,
        List<OrderPaymentResponse> payments) {
}
