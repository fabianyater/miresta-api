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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CashShiftService {

    private static final String CASH_METHOD = "Efectivo";
    private static final String ENTRADA = "ENTRADA";
    private static final String SALIDA = "SALIDA";

    private final CashShiftRepository shiftRepository;
    private final CashMovementRepository movementRepository;
    private final CashShiftMethodClosingRepository methodClosingRepository;
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

    /**
     * Cierra el turno cuadrando TODOS los métodos de pago que tuvieron ventas en el
     * turno (no solo efectivo) — efectivo se cuenta físicamente; tarjeta/transferencia
     * se verifican contra lo que reporte el datáfono o el banco. Un método que no
     * venga en {@code countedByMethod} se asume cuadrado (contado = esperado), para no
     * bloquear el cierre por un método que nadie verificó a mano.
     */
    @Transactional
    public CashShiftResponse close(Long shiftId, Map<String, Long> countedByMethod, String notes, String actingUserEmail) {
        CashShift shift = requireOpenShift(shiftId);
        Instant now = Instant.now();

        Map<String, Long> expectedByMethod = expectedByMethod(shift, now);
        Map<String, Long> counted = countedByMethod != null ? countedByMethod : Map.of();

        List<CashShiftMethodClosing> closings = new ArrayList<>();
        long totalExpected = 0L;
        long totalCounted = 0L;
        Long cashCounted = null;
        Long cashExpected = null;
        for (Map.Entry<String, Long> entry : expectedByMethod.entrySet()) {
            String method = entry.getKey();
            long expected = entry.getValue();
            long countedAmount = counted.getOrDefault(method, expected);
            if (countedAmount < 0) {
                throw new IllegalStateException("El monto contado de " + method + " no puede ser negativo.");
            }

            CashShiftMethodClosing closing = new CashShiftMethodClosing();
            closing.setShift(shift);
            closing.setPaymentTypeName(method);
            closing.setExpectedAmount(expected);
            closing.setCountedAmount(countedAmount);
            closing.setDifferenceAmount(countedAmount - expected);
            closings.add(closing);

            totalExpected += expected;
            totalCounted += countedAmount;
            if (CASH_METHOD.equals(method)) {
                cashExpected = expected;
                cashCounted = countedAmount;
            }
        }
        methodClosingRepository.saveAll(closings);

        shift.setClosedAt(now);
        shift.setClosedBy(resolveName(actingUserEmail));
        shift.setExpectedCash(cashExpected != null ? cashExpected : 0L);
        shift.setCountedCash(cashCounted != null ? cashCounted : 0L);
        shift.setDifference((cashCounted != null ? cashCounted : 0L) - (cashExpected != null ? cashExpected : 0L));
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

    /**
     * Lo esperado por método de pago dentro del turno, hasta {@code asOf}. Efectivo
     * siempre aparece (aunque no haya vendido nada en efectivo, la base y los
     * movimientos igual cuentan); el resto de métodos solo si tuvieron ventas.
     */
    private Map<String, Long> expectedByMethod(CashShift shift, Instant asOf) {
        List<CashMovement> movements = movementRepository.findByShift_IdOrderByCreatedAtAsc(shift.getId());
        long entradas = sumByType(movements, ENTRADA);
        long salidas = sumByType(movements, SALIDA);

        Map<String, Long> salesByMethod = new LinkedHashMap<>();
        for (var row : orderPaymentRepository.findPaymentTotals(shift.getOpenedAt(), asOf)) {
            String method = row.paymentTypeName() != null ? row.paymentTypeName() : "Sin especificar";
            long total = row.total() != null ? row.total() : 0L;
            salesByMethod.merge(method, total, Long::sum);
        }

        Map<String, Long> expected = new LinkedHashMap<>();
        long cashSales = salesByMethod.getOrDefault(CASH_METHOD, 0L);
        expected.put(CASH_METHOD, shift.getOpeningCash().amount() + cashSales + entradas - salidas);
        salesByMethod.forEach((method, total) -> {
            if (!CASH_METHOD.equals(method)) {
                expected.put(method, total);
            }
        });
        return expected;
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

        List<MethodReconciliationResponse> methodReconciliations;
        long totalExpected;
        Money totalCountedMoney;
        Money totalDifferenceMoney;
        if (shift.getClosedAt() != null) {
            List<CashShiftMethodClosing> closings = methodClosingRepository.findByShift_Id(shift.getId());
            methodReconciliations = closings.stream()
                    .map(c -> new MethodReconciliationResponse(
                            c.getPaymentTypeName(),
                            Money.of(c.getExpectedAmount()),
                            Money.of(c.getCountedAmount()),
                            Money.of(c.getDifferenceAmount())))
                    .toList();
            totalExpected = closings.stream().mapToLong(CashShiftMethodClosing::getExpectedAmount).sum();
            long totalCounted = closings.stream().mapToLong(CashShiftMethodClosing::getCountedAmount).sum();
            totalCountedMoney = Money.of(totalCounted);
            totalDifferenceMoney = Money.of(totalCounted - totalExpected);
        } else {
            Map<String, Long> expected = expectedByMethod(shift, Instant.now());
            methodReconciliations = expected.entrySet().stream()
                    .map(e -> new MethodReconciliationResponse(e.getKey(), Money.of(e.getValue()), null, null))
                    .toList();
            totalExpected = expected.values().stream().mapToLong(Long::longValue).sum();
            totalCountedMoney = null;
            totalDifferenceMoney = null;
        }

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
                movements,
                methodReconciliations,
                Money.of(totalExpected),
                totalCountedMoney,
                totalDifferenceMoney);
    }
}
