package com.miresta.table;

/** {@code salonId} es obligatorio al crear; al renombrar, si viene, mueve la mesa a
 * ese salón (conserva su posición actual — se reacomoda a mano en el nuevo plano). */
public record TableRequest(Long number, Long salonId) {
}
