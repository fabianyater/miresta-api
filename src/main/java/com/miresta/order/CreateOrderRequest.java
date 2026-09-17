package com.miresta.order;

import java.util.List;

public record CreateOrderRequest(
        List<OrderRequest> orders,
        Long tableId,
        Long customerId,
        // Para agregarle más platos a un pedido para llevar ya pendiente (sin mesa,
        // así que no hay un tableId con el cual encontrarlo solo) — si viene, se
        // reusa ese pedido en vez de crear uno nuevo. Ignorado si tableId viene.
        Long orderId
) {
}
