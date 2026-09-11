package com.miresta.cashregister;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * El cuadre de un método de pago dentro de un cierre de turno — efectivo se cuenta
 * físicamente; tarjeta/transferencia se verifican contra lo que reporte el datáfono o
 * el banco. Una fila por método, guardada solo al cerrar (es una foto fija, no se
 * recalcula si cambian ventas después).
 */
@Getter
@Setter
@Entity
@Table(name = "cash_shift_method_closings")
public class CashShiftMethodClosing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private CashShift shift;

    @Column(name = "payment_type_name", nullable = false)
    private String paymentTypeName;

    @Column(name = "expected_amount", nullable = false)
    private long expectedAmount;

    @Column(name = "counted_amount", nullable = false)
    private long countedAmount;

    @Column(name = "difference_amount", nullable = false)
    private long differenceAmount;
}
