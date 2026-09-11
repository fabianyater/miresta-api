package com.miresta.cashregister;

import com.miresta.auth.User;
import com.miresta.auth.UserRepository;
import com.miresta.order.OrderPaymentRepository;
import com.miresta.order.PaymentTotalResponse;
import com.miresta.shared.Money;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CashShiftService {

    private static final String CASH_METHOD = "Efectivo";
    private static final String ENTRADA = "ENTRADA";
    private static final String SALIDA = "SALIDA";

    private final CashShiftRepository shiftRepository;
    private final CashMovementRepository movementRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final UserRepository userRepository;

    @Transactional
    public CashShiftResponse open(long openingCash, String actingUserEmail) {
        if (openingCash < 0) {
            throw new IllegalStateException("La base inicial no puede ser negativa.");
        }
        if (shiftRepository.findByClosedAtIsNull().isPresent()) {
            throw new IllegalStateException("Ya hay un turno de caja abierto.");
        }

        CashShift shift = new CashShift();
        shift.setOpenedAt(Instant.now());
        shift.setOpenedBy(resolveName(actingUserEmail));
        shift.setOpeningCash(Money.of(openingCash));

        return toResponse(shiftRepository.save(shift));
    }

    @Transactional
    public CashShiftResponse close(Long shiftId, long countedCash, String notes, String actingUserEmail) {
        if (countedCash < 0) {
            throw new IllegalStateException("El efectivo contado no puede ser negativo.");
        }
        CashShift shift = requireOpenShift(shiftId);
        Instant now = Instant.now();
        long expected = computeExpectedCash(shift, now);

        shift.setClosedAt(now);
        shift.setClosedBy(resolveName(actingUserEmail));
        shift.setCountedCash(countedCash);
        shift.setExpectedCash(expected);
        shift.setDifference(countedCash - expected);
        shift.setNotes(notes != null && !notes.isBlank() ? notes.trim() : null);

        return toResponse(shiftRepository.save(shift));
    }

    @Transactional
    public CashShiftResponse addMovement(Long shiftId, String type, long amount, String reason, String actingUserEmail) {
        String normalizedType = type != null ? type.trim().toUpperCase() : "";
        if (!ENTRADA.equals(normalizedType) && !SALIDA.equals(normalizedType)) {
            throw new IllegalStateException("El tipo de movimiento debe ser ENTRADA o SALIDA.");
        }
        if (amount <= 0) {
            throw new IllegalStateException("El monto debe ser mayor a 0.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalStateException("Indica el motivo del movimiento.");
        }

        CashShift shift = requireOpenShift(shiftId);

        CashMovement movement = new CashMovement();
        movement.setShift(shift);
        movement.setType(normalizedType);
        movement.setAmount(Money.of(amount));
        movement.setReason(reason.trim());
        movement.setCreatedAt(Instant.now());
        movement.setCreatedBy(resolveName(actingUserEmail));
        movementRepository.save(movement);

        return toResponse(shift);
    }

    @Transactional(readOnly = true)
    public CashShiftResponse getCurrent() {
        return shiftRepository.findByClosedAtIsNull().map(this::toResponse).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<CashShiftResponse> list() {
        return shiftRepository.findAllByOrderByOpenedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CashShiftResponse getById(Long id) {
        return shiftRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Turno no encontrado: " + id));
    }

    private CashShift requireOpenShift(Long shiftId) {
        CashShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Turno no encontrado: " + shiftId));
        if (shift.getClosedAt() != null) {
            throw new IllegalStateException("Este turno ya está cerrado.");
        }
        return shift;
    }

    private long computeExpectedCash(CashShift shift, Instant asOf) {
        List<CashMovement> movements = movementRepository.findByShift_IdOrderByCreatedAtAsc(shift.getId());
        long cashSales = cashSalesTotal(shift, asOf);
        long entradas = sumByType(movements, ENTRADA);
        long salidas = sumByType(movements, SALIDA);
        return shift.getOpeningCash().amount() + cashSales + entradas - salidas;
    }

    private long cashSalesTotal(CashShift shift, Instant asOf) {
        return orderPaymentRepository.findPaymentTotals(shift.getOpenedAt(), asOf).stream()
                .filter(row -> CASH_METHOD.equalsIgnoreCase(row.paymentTypeName()))
                .mapToLong(row -> row.total() != null ? row.total() : 0L)
                .sum();
    }

    private long sumByType(List<CashMovement> movements, String type) {
        return movements.stream()
                .filter(m -> type.equalsIgnoreCase(m.getType()))
                .mapToLong(m -> m.getAmount() != null ? m.getAmount().amount() : 0L)
                .sum();
    }

    private String resolveName(String email) {
        return userRepository.findByEmail(email)
                .map(User::getDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .orElse(email);
    }

    private CashShiftResponse toResponse(CashShift shift) {
        Instant asOf = shift.getClosedAt() != null ? shift.getClosedAt() : Instant.now();

        List<PaymentTotalResponse> salesByMethod = orderPaymentRepository.findPaymentTotals(shift.getOpenedAt(), asOf).stream()
                .map(row -> new PaymentTotalResponse(
                        row.paymentTypeName() != null ? row.paymentTypeName() : "Sin especificar",
                        row.orderCount(),
                        Money.of(row.total() != null ? row.total() : 0L)))
                .toList();
        long cashSales = salesByMethod.stream()
                .filter(r -> CASH_METHOD.equalsIgnoreCase(r.paymentTypeName()))
                .mapToLong(r -> r.total().amount())
                .sum();

        List<CashMovement> movementEntities = movementRepository.findByShift_IdOrderByCreatedAtAsc(shift.getId());
        long entradas = sumByType(movementEntities, ENTRADA);
        long salidas = sumByType(movementEntities, SALIDA);

        // Ya cerrado: usa lo que quedó guardado en el cierre. Sigue abierto: se calcula
        // en vivo, sin guardar nada todavía.
        long expectedLive = shift.getExpectedCash() != null
                ? shift.getExpectedCash()
                : shift.getOpeningCash().amount() + cashSales + entradas - salidas;

        List<CashMovementResponse> movements = movementEntities.stream()
                .map(m -> new CashMovementResponse(
                        m.getId(), m.getType(), m.getAmount(), m.getReason(), m.getCreatedAt(), m.getCreatedBy()))
                .toList();

        return new CashShiftResponse(
                shift.getId(),
                shift.getOpenedAt(),
                shift.getOpenedBy(),
                shift.getOpeningCash(),
                shift.getClosedAt(),
                shift.getClosedBy(),
                shift.getCountedCash() != null ? Money.of(shift.getCountedCash()) : null,
                Money.of(expectedLive),
                shift.getDifference() != null ? Money.of(shift.getDifference()) : null,
                shift.getNotes(),
                Money.of(cashSales),
                salesByMethod,
                Money.of(entradas),
                Money.of(salidas),
                movements);
    }
}
