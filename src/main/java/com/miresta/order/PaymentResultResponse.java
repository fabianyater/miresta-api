package com.miresta.order;

/**
 * Resultado de cobrar un pedido. {@code change} es lo que hay que devolverle al
 * cliente cuando pagó de más (normalmente en efectivo); 0 si pagó justo. Lo que
 * queda registrado en caja es siempre {@code total}, no lo que entregó.
 */
public record PaymentResultResponse(long total, long tendered, long change) {
}
