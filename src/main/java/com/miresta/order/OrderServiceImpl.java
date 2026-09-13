package com.miresta.order;

import com.miresta.auth.User;
import com.miresta.auth.UserRepository;
import com.miresta.catalog.Product;
import com.miresta.catalog.ProductBatchServiceImpl;
import com.miresta.customer.Customer;
import com.miresta.customer.CustomerResponse;
import com.miresta.customer.ICustomerService;
import com.miresta.menu.MenuItemServiceImpl;
import com.miresta.menu.MenuOfferingResponse;
import com.miresta.order.pricing.PricingCalculator;
import com.miresta.shared.ComboCategory;
import com.miresta.shared.Money;
import com.miresta.table.DiningTable;
import com.miresta.table.DiningTableRepository;
import com.miresta.table.DiningTableResponse;
import com.miresta.table.TableServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements IOrderService {

    private static final ZoneId RESTAURANT_ZONE = ZoneId.of("America/Bogota");

    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final DiningTableRepository diningTableRepository;
    private final ICustomerService customerService;
    private final IPaymentTypeService paymentTypeService;
    private final OrderItemServiceImpl orderItemService;
    private final OrderItemSelectionsImpl orderItemSelectionsService;
    private final TableServiceImpl tableService;
    private final MenuItemServiceImpl menuItemService;
    private final ProductBatchServiceImpl productBatchService;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemSelectionsRepository orderItemSelectionsRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final PricingCalculator pricingCalculator;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public void createOrder(CreateOrderRequest orderRequest) {
        String waiterName = currentWaiterName();
        Customer customer = orderRequest.customerId() != null
                ? customerService.getCustomerById(orderRequest.customerId())
                : null;

        Order savedOrder;

        if (orderRequest.tableId() != null) {
            // Locked, not a plain findById — otherwise two near-simultaneous requests
            // for the same table (two waiters, or a retry) can both see "no pending
            // order yet" before either commits, and each creates its own separate
            // ticket instead of sharing one. The second request just waits here for the
            // first to finish, then correctly finds and reuses what it created.
            DiningTable diningTable = diningTableRepository.findByIdForUpdate(orderRequest.tableId())
                    .orElseThrow(() -> new RuntimeException("Dining table not found: " + orderRequest.tableId()));

            Optional<Order> existingOrder = orderRepository.findByDiningTable_IdAndDiningTable_Status_NameAndOrderStatus_Name(
                    orderRequest.tableId(), "IN_USE", "PENDING");

            if (existingOrder.isPresent()) {
                savedOrder = existingOrder.get();
                // Un pedido pendiente puede recibir varias tandas de items (mesa que sigue
                // pidiendo) — si esta tanda trae un cliente, se registra en el pedido aunque
                // ya existiera, en vez de descartarlo silenciosamente.
                if (customer != null) {
                    savedOrder.setCustomer(customer);
                }
            } else {
                tableService.updateTableStatus(diningTable, "IN_USE");

                Order order = new Order();
                order.setCreatedAt(Instant.now());
                order.setDiningTable(diningTable);
                order.setOrderStatus(orderStatusRepository.findByName("PENDING"));
                order.setCustomer(customer);
                order.setWaiterName(waiterName);

                savedOrder = orderRepository.save(order);
            }
        } else {
            Order order = new Order();
            order.setCreatedAt(Instant.now());
            order.setOrderStatus(orderStatusRepository.findByName("PENDING"));
            order.setCustomer(customer);
            order.setWaiterName(waiterName);

            savedOrder = orderRepository.save(order);
        }

        for (OrderRequest o : orderRequest.orders()) {
            int repetitions = (o.count() != null && o.count() > 0) ? o.count() : 1;

            // This plato's own customer, if it brought one — otherwise falls back to the
            // request's top-level customer, so the common single-customer case (every
            // plato in this submission is for the same person) still works unchanged.
            Customer itemCustomer = o.customerId() != null
                    ? customerService.getCustomerById(o.customerId())
                    : customer;

            for (int i = 0; i < repetitions; i++) {
                OrderItem orderItem = orderItemService.createOrderItem(
                        savedOrder, o.menuId(), o.mealType(), o.isToGo(), o.comments(), itemCustomer);
                orderItemSelectionsService.createOrderItemSelection(orderItem, o.items());
                orderItemService.updateTotalPrice(orderItem);
            }
        }

        calculateTotals(savedOrder);
        orderRepository.save(savedOrder);
    }

    @Override
    public List<OrdersResponse> getOrders(String status, Long customerId) {
        List<Order> orders;

        if (customerId != null) {
            orders = orderRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId);
            if (status != null && !status.isBlank()) {
                orders = orders.stream()
                        .filter(o -> o.getOrderStatus() != null && status.equalsIgnoreCase(o.getOrderStatus().getName()))
                        .toList();
            }
        } else {
            orders = orderRepository.findByOrderStatus_Name(status);
        }

        return orders.stream()
                .map(this::mapOrdersResponse)
                .sorted(Comparator.comparing(OrdersResponse::createdAt).reversed())
                .toList();
    }

    @Override
    public List<OrdersResponse> getOrderHistory(LocalDate date) {
        Instant from = date.atStartOfDay(RESTAURANT_ZONE).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(RESTAURANT_ZONE).toInstant();

        return orderRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to).stream()
                .map(this::mapOrdersResponse)
                .toList();
    }

    private OrdersResponse mapOrdersResponse(Order order) {
        return new OrdersResponse(
                order.getId(),
                order.getCreatedAt(),
                order.getNotes(),
                order.getSubtotal(),
                order.getTotal(),
                mapDiningTable(order.getDiningTable()),
                mapOrderStatus(order.getOrderStatus()),
                mapCustomer(order.getCustomer()),
                mapPaymentType(order.getPaymentType()),
                order.getPaidAt() != null,
                mapPayments(order.getId()),
                order.getWaiterName()
        );
    }

    @Transactional(readOnly = true)
    @Override
    public OrderDetailsResponse getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada con id: " + orderId));

        return new OrderDetailsResponse(
                order.getId(),
                order.getCreatedAt(),
                order.getNotes(),
                order.getSubtotal(),
                order.getTotal(),
                mapDiningTable(order.getDiningTable()),
                mapOrderStatus(order.getOrderStatus()),
                mapCustomer(order.getCustomer()),
                mapPaymentType(order.getPaymentType()),
                order.getPaidAt() != null,
                order.getOrderItems().stream()
                        .sorted(Comparator.comparing(OrderItem::getId).reversed())
                        .map(this::mapOrderItem)
                        .toList(),
                mapPayments(order.getId()),
                order.getWaiterName()
        );
    }

    @Override
    public OrderDetailsResponse getPendingOrderDetail(Long tableId) {
        Order order = orderRepository.findByDiningTable_IdAndDiningTable_Status_NameAndOrderStatus_Name(
                        tableId, "IN_USE", "PENDING")
                .orElse(null);

        if (order == null) {
            return null;
        }

        return new OrderDetailsResponse(
                order.getId(),
                order.getCreatedAt(),
                order.getNotes(),
                order.getSubtotal(),
                order.getTotal(),
                mapDiningTable(order.getDiningTable()),
                mapOrderStatus(order.getOrderStatus()),
                mapCustomer(order.getCustomer()),
                mapPaymentType(order.getPaymentType()),
                order.getPaidAt() != null,
                order.getOrderItems().stream()
                        .map(this::mapOrderItem)
                        .toList(),
                mapPayments(order.getId()),
                order.getWaiterName()
        );
    }

    @Transactional
    @Override
    public PaymentResultResponse updateOrderStatus(Long orderId, String status, List<PaymentLine> payments) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));

        OrderStatus newStatus = orderStatusRepository.findByName(status);
        if (newStatus == null) {
            throw new EntityNotFoundException("Order status not found: " + status);
        }

        boolean wasAlreadyCancelled = "CANCELLED".equalsIgnoreCase(order.getOrderStatus().getName());

        if (wasAlreadyCancelled && "COMPLETED".equalsIgnoreCase(status)) {
            // A cancelled order already had its reserved stock given back — completing
            // (charging for) it afterward would bill for something that was voided.
            throw new IllegalStateException("Este pedido está cancelado, no se puede cobrar.");
        }

        order.setOrderStatus(newStatus);

        PaymentResultResponse paymentResult = null;
        if ("COMPLETED".equalsIgnoreCase(status)) {
            if (payments != null && !payments.isEmpty()) {
                paymentResult = applyPayments(order, payments);
            } else if (order.getCustomer() != null) {
                // Fiado: served and the table is freed, but billed to the customer's
                // open tab — stays unpaid until settled later (payOrder/settleCustomerTab).
                order.setPaymentType(null);
                order.setPaidAt(null);
            } else {
                throw new IllegalStateException(
                        "Selecciona un método de pago, o un cliente para dejar la cuenta abierta.");
            }
        }

        orderRepository.save(order);

        if ("COMPLETED".equalsIgnoreCase(status)) {
            Optional.ofNullable(order.getDiningTable()).ifPresent(diningTable ->
                    tableService.updateTableStatus(diningTable, "OPEN"));
        }

        if ("CANCELLED".equalsIgnoreCase(status) && !wasAlreadyCancelled) {
            // Symmetric to the consume() calls in OrderItemSelectionsImpl#createOrderItemSelection —
            // give back whatever menu-item and lote stock this order had reserved.
            for (OrderItem item : orderItemService.getOrderItemsByOrder(order)) {
                for (OrderItemSelection selection : orderItemSelectionsService.getOrderItemSelectionByOrderItem(item)) {
                    menuItemService.restore(item.getMenuOffering(), selection.getProduct(), selection.getQuantity());
                    productBatchService.restore(selection.getProduct(), selection.getQuantity());
                }
            }

            // A cancelled order is no longer occupying the table either.
            Optional.ofNullable(order.getDiningTable()).ifPresent(diningTable ->
                    tableService.updateTableStatus(diningTable, "OPEN"));
        }

        return paymentResult;
    }

    @Transactional
    @Override
    public PaymentResultResponse payOrder(Long orderId, List<PaymentLine> payments) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));

        if (!"COMPLETED".equalsIgnoreCase(order.getOrderStatus().getName())) {
            throw new IllegalStateException("Solo se puede pagar un pedido ya completado.");
        }
        if (order.getPaidAt() != null) {
            throw new IllegalStateException("Este pedido ya fue pagado.");
        }

        PaymentResultResponse result = applyPayments(order, payments);
        orderRepository.save(order);
        return result;
    }

    /**
     * Registra uno o más pagos (ej. una parte en efectivo, el resto por transferencia)
     * contra un pedido y lo marca pagado — usado por payOrder y por el cobro directo en
     * updateOrderStatus.
     *
     * <p>Acepta que el cliente pague de más: lo que sobra del total se devuelve como
     * cambio y NO se registra en caja. Los pagos se aplican en orden hasta cubrir el
     * total; una línea que quede por encima entra recortada (o no entra, si el total ya
     * estaba cubierto). Si los pagos no alcanzan el total, sí falla.
     */
    private PaymentResultResponse applyPayments(Order order, List<PaymentLine> payments) {
        if (payments == null || payments.isEmpty()) {
            throw new IllegalStateException("Debes indicar al menos un método de pago.");
        }
        if (payments.stream().anyMatch(p -> p.amount() == null || p.amount() <= 0)) {
            throw new IllegalStateException("Cada pago debe tener un monto mayor a 0.");
        }

        long due = order.getTotal() != null ? order.getTotal().amount() : 0L;
        long tendered = payments.stream().mapToLong(PaymentLine::amount).sum();
        if (tendered < due) {
            throw new IllegalStateException(
                    "Los pagos (" + tendered + ") no alcanzan el total a cobrar (" + due + ").");
        }

        Instant now = Instant.now();
        List<PaymentType> resolvedTypes = new ArrayList<>();
        long remaining = due;
        for (PaymentLine line : payments) {
            long applied = Math.min(line.amount(), remaining);
            remaining -= applied;
            if (applied <= 0) {
                // El total ya estaba cubierto por las líneas anteriores — esta línea fue
                // toda vuelto (efectivo que se devuelve), no se registra.
                continue;
            }

            PaymentType paymentType = paymentTypeService.getPaymentTypeById(line.paymentTypeId());
            resolvedTypes.add(paymentType);

            OrderPayment payment = new OrderPayment();
            payment.setOrder(order);
            payment.setPaymentType(paymentType);
            payment.setAmount(Money.of(applied));
            payment.setPaidAt(now);
            orderPaymentRepository.save(payment);
        }

        // Un solo método -> se refleja también aquí (compatibilidad con lo que ya leía
        // order.paymentType); dos o más -> queda null, el desglose real vive en payments.
        order.setPaymentType(resolvedTypes.size() == 1 ? resolvedTypes.get(0) : null);
        order.setPaidAt(now);
        return new PaymentResultResponse(due, tendered, tendered - due);
    }

    @Transactional
    @Override
    public OrderDetailsResponse fiarCliente(Long orderId, Long customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada con id: " + orderId));

        if (!"PENDING".equalsIgnoreCase(order.getOrderStatus().getName())) {
            throw new IllegalStateException("Solo se puede fiar un pedido que sigue pendiente.");
        }

        Customer customer = customerService.getCustomerById(customerId);

        List<OrderItem> allItems = orderItemService.getOrderItemsByOrder(order);
        List<OrderItem> toMove = allItems.stream()
                .filter(item -> {
                    Customer effective = item.getCustomer() != null ? item.getCustomer() : order.getCustomer();
                    return effective != null && effective.getId().equals(customerId);
                })
                .toList();

        if (toMove.isEmpty()) {
            throw new IllegalStateException("Este cliente no tiene platos en este pedido.");
        }

        // The new tab keeps the same table/creation time (for history — "this fiado tab
        // came from Mesa 3, ordered at 1pm") even though it's no longer occupying the
        // table itself; only the ORIGINAL order's remaining items (if any) do that.
        Order tabOrder = new Order();
        tabOrder.setCreatedAt(order.getCreatedAt());
        tabOrder.setDiningTable(order.getDiningTable());
        tabOrder.setOrderStatus(orderStatusRepository.findByName("COMPLETED"));
        tabOrder.setCustomer(customer);
        tabOrder.setWaiterName(order.getWaiterName());
        tabOrder = orderRepository.save(tabOrder);

        long movedTotal = 0L;
        for (OrderItem item : toMove) {
            movedTotal += item.getTotal() != null ? item.getTotal().amount() : 0L;
            item.setOrder(tabOrder);
        }
        orderItemRepository.saveAll(toMove);
        tabOrder.setSubtotal(Money.of(movedTotal));
        tabOrder.setTotal(Money.of(movedTotal));
        orderRepository.save(tabOrder);

        // Changing item.order's FK doesn't retroactively update order's own in-memory
        // orderItems collection (same persistence context, so a later findById(orderId)
        // — e.g. from getOrderDetail below — returns this exact stale collection instead
        // of re-querying) — drop the moved items from it explicitly so the response
        // reflects reality.
        order.getOrderItems().removeAll(toMove);

        boolean movedEverything = toMove.size() == allItems.size();
        if (movedEverything) {
            // Nothing of its own left on the original ticket — it's fully resolved (all
            // of it just became the new tab), so it's closed out and the table freed,
            // same as if it had been cancelled.
            order.setOrderStatus(orderStatusRepository.findByName("CANCELLED"));
            order.setSubtotal(Money.ZERO);
            order.setTotal(Money.ZERO);
            Optional.ofNullable(order.getDiningTable())
                    .ifPresent(diningTable -> tableService.updateTableStatus(diningTable, "OPEN"));
        } else {
            long remainingTotal = allItems.stream()
                    .filter(item -> !toMove.contains(item))
                    .mapToLong(item -> item.getTotal() != null ? item.getTotal().amount() : 0L)
                    .sum();
            order.setSubtotal(Money.of(remainingTotal));
            order.setTotal(Money.of(remainingTotal));
        }
        orderRepository.save(order);

        return getOrderDetail(order.getId());
    }

    @Transactional
    @Override
    public SettleTabResponse settleCustomerTab(Long customerId, Long paymentTypeId) {
        if (paymentTypeId == null) {
            throw new IllegalStateException("Debes indicar un método de pago.");
        }

        List<Order> pending = orderRepository.findByCustomer_IdAndOrderStatus_NameAndPaidAtIsNull(customerId, "COMPLETED");
        if (pending.isEmpty()) {
            throw new IllegalStateException("Este cliente no tiene cuentas pendientes por pagar.");
        }

        PaymentType paymentType = paymentTypeService.getPaymentTypeById(paymentTypeId);
        Instant now = Instant.now();
        long totalPaid = 0L;

        for (Order order : pending) {
            long amount = order.getTotal() != null ? order.getTotal().amount() : 0L;

            OrderPayment payment = new OrderPayment();
            payment.setOrder(order);
            payment.setPaymentType(paymentType);
            payment.setAmount(Money.of(amount));
            payment.setPaidAt(now);
            orderPaymentRepository.save(payment);

            order.setPaymentType(paymentType);
            order.setPaidAt(now);
            totalPaid += amount;
        }

        orderRepository.saveAll(pending);

        return new SettleTabResponse(pending.size(), Money.of(totalPaid));
    }

    @Transactional(readOnly = true)
    @Override
    public List<CustomerPaymentResponse> getCustomerPayments(Long customerId) {
        // Se agrupan por (momento del cobro, método): un pago de cuenta genera varias
        // filas OrderPayment con el mismo instante y método; pagar un pedido suelto es
        // su propio grupo.
        Map<String, List<OrderPayment>> groups = new LinkedHashMap<>();
        for (OrderPayment payment : orderPaymentRepository.findByOrder_Customer_IdOrderByPaidAtDesc(customerId)) {
            String key = payment.getPaidAt() + "|" + payment.getPaymentType().getName();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(payment);
        }

        List<CustomerPaymentResponse> result = new ArrayList<>();
        for (List<OrderPayment> group : groups.values()) {
            long total = group.stream().mapToLong(p -> p.getAmount() != null ? p.getAmount().amount() : 0L).sum();
            List<CustomerPaymentResponse.PaidOrder> orders = group.stream()
                    .map(p -> new CustomerPaymentResponse.PaidOrder(
                            p.getOrder().getId(),
                            p.getOrder().getCreatedAt(),
                            p.getAmount(),
                            mapDiningTable(p.getOrder().getDiningTable())))
                    .toList();
            result.add(new CustomerPaymentResponse(
                    group.get(0).getPaidAt(),
                    group.get(0).getPaymentType().getName(),
                    Money.of(total),
                    orders));
        }
        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public List<CustomerBalanceResponse> getCustomerBalances() {
        return orderRepository.findCustomerBalances().stream()
                .map(row -> new CustomerBalanceResponse(
                        row.customerId(),
                        row.customerName(),
                        row.pendingOrders(),
                        Money.of(row.totalOwed() != null ? row.totalOwed() : 0L)))
                .toList();
    }

    @Override
    public List<PaymentTotalResponse> getPaymentTotals(LocalDate date) {
        Instant from = date.atStartOfDay(RESTAURANT_ZONE).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(RESTAURANT_ZONE).toInstant();

        return orderPaymentRepository.findPaymentTotals(from, to).stream()
                .map(row -> new PaymentTotalResponse(
                        row.paymentTypeName() != null ? row.paymentTypeName() : "Sin especificar",
                        row.orderCount(),
                        Money.of(row.total() != null ? row.total() : 0L)))
                .toList();
    }

    @Override
    public DailyReportResponse getDailyReport(LocalDate date) {
        Instant from = date.atStartOfDay(RESTAURANT_ZONE).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(RESTAURANT_ZONE).toInstant();

        DaySummaryRow summary = orderRepository.findDaySummary(from, to);
        OpenTabsRow openTabs = orderRepository.findOpenTabsInRange(from, to);
        Long additionsQty = orderItemSelectionsRepository.sumSelectionQuantityByCategory(from, to, ComboCategory.ADICIONAL);

        List<DailyReportResponse.MealTypeSummary> mealTypeCounts = orderItemRepository.findMealTypeCounts(from, to).stream()
                .map(row -> new DailyReportResponse.MealTypeSummary(
                        row.foodType(), row.count(), Money.of(row.total() != null ? row.total() : 0L)))
                .toList();

        List<DailyReportResponse.FulfillmentSummary> fulfillmentCounts = orderItemRepository.findFulfillmentCounts(from, to).stream()
                .map(row -> new DailyReportResponse.FulfillmentSummary(
                        "OUT".equals(row.orderTypeName()) ? "PARA_LLEVAR" : "EN_SITIO",
                        row.count(),
                        Money.of(row.total() != null ? row.total() : 0L)))
                .toList();

        Map<Integer, Long> byHour = orderRepository.findOrderCreatedTimesInRange(from, to).stream()
                .collect(Collectors.groupingBy(ts -> ts.atZone(RESTAURANT_ZONE).getHour(), Collectors.counting()));
        List<DailyReportResponse.HourlyCount> hourlyCounts = byHour.entrySet().stream()
                .map(e -> new DailyReportResponse.HourlyCount(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(DailyReportResponse.HourlyCount::hour))
                .toList();

        return new DailyReportResponse(
                summary.totalOrders(),
                summary.cancelledOrders(),
                Money.of(summary.totalSales() != null ? summary.totalSales() : 0L),
                summary.registeredCustomers(),
                openTabs.count(),
                Money.of(openTabs.total() != null ? openTabs.total() : 0L),
                additionsQty != null ? additionsQty : 0L,
                mealTypeCounts,
                fulfillmentCounts,
                hourlyCounts);
    }

    private void calculateTotals(Order order) {
        List<OrderItem> items = orderItemService.getOrderItemsByOrder(order);

        long orderSubtotal = 0L;

        for (OrderItem oi : items) {
            orderSubtotal += oi.getTotal() != null ? oi.getTotal().amount() : 0L;
        }

        order.setSubtotal(Money.of(orderSubtotal));
        order.setTotal(Money.of(orderSubtotal));
    }

    private DiningTableResponse mapDiningTable(DiningTable diningTable) {
        if (diningTable == null) {
            return null;
        }
        return new DiningTableResponse(
                diningTable.getId(),
                diningTable.getNumber(),
                diningTable.getStatus() != null ? diningTable.getStatus().getName() : null
        );
    }

    private OrderStatusDto mapOrderStatus(OrderStatus orderStatus) {
        return new OrderStatusDto(
                orderStatus.getId(),
                orderStatus.getName()
        );
    }

    private OrderItemProductResponse toProductResponse(OrderItemSelection selection, String mealType, boolean comboFormed) {
        return new OrderItemProductResponse(
                selection.getProduct().getId(),
                selection.getProduct().getName(),
                selection.getQuantity(),
                selection.getUnitExtraPrice(),
                Money.of(Optional.ofNullable(selection.getProduct().getProductDetails())
                        .map(details -> details.stream()
                                .map(d -> d.getPrice() != null ? d.getPrice().amount() : 0L)
                                .reduce(0L, Long::sum))
                        .orElse(0L)),
                selection.getReplacementCategory(),
                pricingCalculator.lineTotalFor(selection, mealType, comboFormed)
        );
    }

    private CustomerResponse mapCustomer(Customer customer) {
        if (customer == null) {
            return null;
        }
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getPhone(), customer.isActive());
    }

    private static final Map<ComboCategory, String> COMBO_CATEGORY_LABELS = Map.of(
            ComboCategory.SOPA, "Sopas",
            ComboCategory.PRINCIPIO, "Principios",
            ComboCategory.PROTEINA, "Proteínas",
            ComboCategory.ACOMPANANTE, "Acompañantes",
            ComboCategory.ADICIONAL, "Adicionales",
            ComboCategory.BEBIDA, "Bebidas",
            ComboCategory.ESPECIAL, "Especiales",
            ComboCategory.ENVASE, "Envases");

    /** Agrupa por el rol que la selección realmente cumple — si tiene reemplazo (ej. un
     * huevo, de categoría Proteínas, pedido "por principio"), se muestra bajo ese rol en
     * vez de bajo su categoría cruda de catálogo, para que coincida con cómo se cobra. */
    private String displayCategoryFor(OrderItemSelection selection) {
        var rawCategory = selection.getProduct().getCategory();
        ComboCategory effective = pricingCalculator.effectiveCategoryFor(selection);
        if (rawCategory != null && effective == rawCategory.getCode()) {
            return rawCategory.getName();
        }
        return COMBO_CATEGORY_LABELS.getOrDefault(effective, rawCategory != null ? rawCategory.getName() : "Sin categoría");
    }

    private List<OrderPaymentResponse> mapPayments(Long orderId) {
        return orderPaymentRepository.findByOrder_IdOrderByPaidAtAsc(orderId).stream()
                .map(p -> new OrderPaymentResponse(p.getPaymentType().getName(), p.getAmount().amount()))
                .toList();
    }

    private PaymentTypeResponse mapPaymentType(PaymentType paymentType) {
        if (paymentType == null) {
            return null;
        }
        return new PaymentTypeResponse(paymentType.getId(), paymentType.getName());
    }

    private OrderItemResponse mapOrderItem(OrderItem orderItem) {
        String mealType = orderItem.getMenuOffering().getFoodType().getName();
        boolean comboFormed = orderItem.getBaseTotal() != null && orderItem.getBaseTotal().amount() > 0;

        // This plato's own customer if it has one, else whoever the order itself belongs
        // to — a plato created before per-item customers existed (or one that just never
        // got its own) still shows something sensible instead of blank.
        Customer effectiveCustomer = orderItem.getCustomer() != null
                ? orderItem.getCustomer()
                : orderItem.getOrder().getCustomer();

        List<OrderItemSelection> accompanimentSelections = AccompanimentDisplay.accompanimentSelections(orderItem);
        var groupedSelections = AccompanimentDisplay.nonAccompanimentSelections(orderItem).stream()
                .collect(java.util.stream.Collectors.groupingBy(this::displayCategoryFor));

        List<GroupedOrderItemResponse> itemsByCategory = new ArrayList<>(groupedSelections.entrySet().stream()
                .map(entry -> new GroupedOrderItemResponse(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(selection -> toProductResponse(selection, mealType, comboFormed))
                                .toList()
                ))
                .toList());

        // Solo lo que se salió de lo normal — ver AccompanimentDisplay.
        List<OrderItemProductResponse> accompanimentDeviations = new ArrayList<>();
        for (OrderItemSelection selection : AccompanimentDisplay.doubled(accompanimentSelections)) {
            accompanimentDeviations.add(toProductResponse(selection, mealType, comboFormed));
        }
        for (Product missing : AccompanimentDisplay.missingProducts(orderItem, accompanimentSelections)) {
            accompanimentDeviations.add(new OrderItemProductResponse(
                    missing.getId(), "Sin " + missing.getName(), 0L, Money.ZERO, Money.ZERO, null, null));
        }
        if (!accompanimentDeviations.isEmpty()) {
            itemsByCategory.add(new GroupedOrderItemResponse(
                    COMBO_CATEGORY_LABELS.get(ComboCategory.ACOMPANANTE), accompanimentDeviations));
        }

        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getComments(),
                orderItem.getComboLabel(),
                mapCustomer(effectiveCustomer),
                orderItem.getBaseTotal(),
                zeroIfNull(orderItem.getDrinksTotal()),
                zeroIfNull(orderItem.getProteinAdditionalsTotal()),
                zeroIfNull(orderItem.getSideAdditionalsTotal()),
                zeroIfNull(orderItem.getExtrasTotal()),
                zeroIfNull(orderItem.getIndividualsTotal()),
                orderItem.getToGoSurcharge(),
                orderItem.getTotal(),
                mapMenuOffering(orderItem.getMenuOffering()),
                mapOrderType(orderItem.getOrderType()),
                itemsByCategory
        );
    }

    // Order items created before the price-breakdown columns existed have null here —
    // treat them as zero rather than leaking null into the API response.
    private Money zeroIfNull(Money money) {
        return money != null ? money : Money.ZERO;
    }

    private MenuOfferingResponse mapMenuOffering(com.miresta.menu.MenuOffering menuOffering) {
        return new MenuOfferingResponse(
                menuOffering.getId(),
                menuOffering.getMenu() != null ? menuOffering.getMenu().getDate() : null,
                menuOffering.getFoodType() != null ? menuOffering.getFoodType().getName() : null
        );
    }

    private OrderTypeDto mapOrderType(OrderType orderType) {
        return new OrderTypeDto(
                orderType.getId(),
                orderType.getName()
        );
    }

    /**
     * Nombre del mesero que está creando el pedido — se guarda como texto en la orden
     * (no como FK) para que quede fijo en el histórico aunque el usuario cambie luego
     * su nombre o lo eliminen. Usa el displayName del usuario; si por algo no se
     * encuentra, cae al correo antes que dejarlo en blanco.
     */
    private String currentWaiterName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof String email)) {
            throw new BadCredentialsException("Session expired or not authenticated");
        }

        return userRepository.findByEmail(email)
                .map(User::getDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .orElse(email);
    }
}
