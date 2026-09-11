package com.miresta.cashregister;

import com.miresta.order.PaymentTotalResponse;
import com.miresta.shared.Money;

import java.time.Instant;
import java.util.List;

/**
 * Mientras el turno sigue abierto, {@code expectedCash} se calcula al vuelo (base +
 * ventas en efectivo + entradas − salidas) y {@code countedCash}/{@code difference}
 * quedan {@code null} — solo se guardan de verdad al cerrar.
 */
public record CashShiftResponse(
        Long id,
        Instant openedAt,
        String openedBy,
        Money openingCash,
        Instant closedAt,
        String closedBy,
        Money countedCash,
        Money expectedCash,
        Money difference,
        String notes,
        Money cashSales,
        List<PaymentTotalResponse> salesByMethod,
        Money totalEntradas,
        Money totalSalidas,
        List<CashMovementResponse> movements,
        // Cuadre de TODOS los métodos de pago, no solo efectivo (que además aparece
        // arriba porque es el único que se cuenta físicamente y tiene base/movimientos).
        List<MethodReconciliationResponse> methodReconciliations,
        Money totalExpectedAllMethods,
        Money totalCountedAllMethods,
        Money totalDifferenceAllMethods) {
}
