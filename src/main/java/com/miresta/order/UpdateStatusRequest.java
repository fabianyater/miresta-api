package com.miresta.order;

/**
 * paymentTypeId is only meaningful (and typically expected) when status is COMPLETED —
 * that's the moment an order actually gets "cobrado".
 */
public record UpdateStatusRequest(String status, Long paymentTypeId) {
}
