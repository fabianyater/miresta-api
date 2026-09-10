package com.miresta.order;

import java.util.List;

/**
 * payments is only meaningful (and typically expected) when status is COMPLETED —
 * that's the moment an order actually gets "cobrado". Can be split across more than
 * one payment type (ej. una parte en efectivo, el resto por transferencia) as long as
 * they add up to the order's total.
 */
public record UpdateStatusRequest(String status, List<PaymentLine> payments) {
}
