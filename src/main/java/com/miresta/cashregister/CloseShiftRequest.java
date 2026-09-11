package com.miresta.cashregister;

import java.util.Map;

/**
 * {@code countedByMethod} trae lo contado/verificado por método de pago (ej.
 * {"Efectivo": 120000, "Tarjeta": 85000}) — efectivo se cuenta físicamente,
 * tarjeta/transferencia se verifican contra el datáfono o el banco. Un método que no
 * venga en el mapa se asume cuadrado (contado = esperado).
 */
public record CloseShiftRequest(Map<String, Long> countedByMethod, String notes) {
}
