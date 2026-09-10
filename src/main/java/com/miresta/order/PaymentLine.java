package com.miresta.order;

/** One line of a (possibly split) payment — ej. $20.000 en efectivo. */
public record PaymentLine(Long paymentTypeId, Long amount) {
}
