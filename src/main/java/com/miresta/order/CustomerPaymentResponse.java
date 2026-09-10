package com.miresta.order;

import com.miresta.shared.Money;
import com.miresta.table.DiningTableResponse;

import java.time.Instant;
import java.util.List;

/**
 * Una "vez que pagó" el cliente: un cobro (fecha + método) y los pedidos que cubrió.
 * Un pago de cuenta ({@code settleCustomerTab}) agrupa varios pedidos; pagar un
 * pedido suelto es un grupo de uno.
 */
public record CustomerPaymentResponse(
        Instant paidAt,
        String paymentTypeName,
        Money total,
        List<PaidOrder> orders) {

    public record PaidOrder(Long orderId, Instant createdAt, Money amount, DiningTableResponse diningTable) {
    }
}
