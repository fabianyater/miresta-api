package com.miresta.cashregister;

import com.miresta.shared.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Un turno de caja: se abre con una base inicial de efectivo y se cierra con un
 * arqueo (lo que debería haber vs. lo que el cajero contó). Mientras está abierto
 * ({@code closedAt == null}), {@code countedCash}/{@code expectedCash}/{@code difference}
 * quedan sin llenar — el service los calcula "en vivo" para mostrarlos sin guardarlos
 * hasta el cierre real.
 */
@Getter
@Setter
@Entity
@Table(name = "cash_shifts")
public class CashShift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "opened_by", nullable = false)
    private String openedBy;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "opening_cash"))
    private Money openingCash;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by")
    private String closedBy;

    @Column(name = "counted_cash")
    private Long countedCash;

    @Column(name = "expected_cash")
    private Long expectedCash;

    @Column(name = "cash_difference")
    private Long difference;

    @Column(name = "notes")
    private String notes;
}
