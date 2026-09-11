package com.miresta.cashregister;

import com.miresta.shared.Money;

/**
 * Mientras el turno sigue abierto, {@code counted}/{@code difference} vienen
 * {@code null} — {@code expected} se calcula en vivo. Al cerrar quedan fijos con lo
 * que se guardó en ese momento.
 */
public record MethodReconciliationResponse(
        String paymentTypeName, Money expected, Money counted, Money difference) {
}
