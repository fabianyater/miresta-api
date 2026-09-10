package com.miresta.printing;

import com.miresta.order.IOrderService;
import com.miresta.order.Order;
import com.miresta.order.OrderItem;
import com.miresta.order.OrderItemSelection;
import com.miresta.order.OrderRepository;
import com.miresta.order.pricing.PricingCalculator;
import com.miresta.shared.ComboCategory;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class TicketService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Orden de impresión de un plato — sopas primero, luego principios, proteínas,
    // acompañantes y el resto (adicionales/bebidas/especiales) al final, sin importar
    // en qué orden se hayan agregado las selecciones al pedido.
    private static final Map<ComboCategory, Integer> PRINT_ORDER = new EnumMap<>(ComboCategory.class);

    static {
        PRINT_ORDER.put(ComboCategory.SOPA, 0);
        PRINT_ORDER.put(ComboCategory.PRINCIPIO, 1);
        PRINT_ORDER.put(ComboCategory.PROTEINA, 2);
        PRINT_ORDER.put(ComboCategory.ACOMPANANTE, 3);
        PRINT_ORDER.put(ComboCategory.ADICIONAL, 4);
        PRINT_ORDER.put(ComboCategory.BEBIDA, 5);
        PRINT_ORDER.put(ComboCategory.ESPECIAL, 6);
    }

    private final OrderRepository orderRepository;
    private final IOrderService orderService;
    private final TicketPrinter ticketPrinter;
    private final PrinterSettingService printerSettingService;
    private final PricingCalculator pricingCalculator;

    public TicketPreviewResponse printComanda(Long orderId) {
        Order order = getOrder(orderId);

        EscPosDocument doc = new EscPosDocument()
                .center().bold(true).line("COMANDA").bold(false)
                .line(tableLabel(order))
                .line(TIME_FORMAT.format(order.getCreatedAt().atZone(java.time.ZoneId.of("America/Bogota"))));

        if (order.getCustomer() != null) {
            doc.line("Cliente: " + order.getCustomer().getName());
        }

        doc.rule().left();

        for (OrderItem item : order.getOrderItems()) {
            doc.bold(true).line(item.getMenuOffering().getFoodType().getName()).bold(false);

            for (OrderItemSelection selection : sortedByPrintOrder(item)) {
                doc.line("  " + selection.getQuantity() + "x " + selection.getProduct().getName());
            }

            if (item.getComments() != null && !item.getComments().isBlank()) {
                doc.line("  Nota: " + item.getComments());
            }
            doc.blankLine();
        }

        doc.cut();
        return finish("Comanda", doc);
    }

    public TicketPreviewResponse printCuenta(Long orderId) {
        Order order = getOrder(orderId);

        EscPosDocument doc = new EscPosDocument()
                .center().bold(true).line("CUENTA").bold(false)
                .line(tableLabel(order))
                .line(TIME_FORMAT.format(order.getCreatedAt().atZone(java.time.ZoneId.of("America/Bogota"))));

        if (order.getCustomer() != null) {
            doc.line("Cliente: " + order.getCustomer().getName());
        }

        doc.rule().left();

        for (OrderItem item : order.getOrderItems()) {
            doc.line(item.getMenuOffering().getFoodType().getName() + " - " + money(item.getTotal()));
            for (OrderItemSelection selection : sortedByPrintOrder(item)) {
                doc.line("  " + selection.getQuantity() + "x " + selection.getProduct().getName());
            }
        }

        doc.rule()
                .line("Subtotal: " + money(order.getSubtotal()))
                .bold(true).line("Total: " + money(order.getTotal())).bold(false)
                .cut();

        return finish("Cuenta", doc);
    }

    public TicketPreviewResponse printResumenDelDia(LocalDate date) {
        var totals = orderService.getPaymentTotals(date);

        EscPosDocument doc = new EscPosDocument()
                .center().bold(true).line("RESUMEN DEL DIA").bold(false)
                .line(date.toString())
                .rule()
                .left();

        long grandTotal = 0L;
        long orderCount = 0L;

        for (var row : totals) {
            doc.line(row.paymentTypeName() + ": " + row.orderCount() + " pedidos - " + money(row.total()));
            grandTotal += row.total().amount();
            orderCount += row.orderCount();
        }

        doc.rule()
                .line("Pedidos: " + orderCount)
                .bold(true).line("Total: " + money(com.miresta.shared.Money.of(grandTotal))).bold(false)
                .cut();

        return finish("Resumen del día", doc);
    }

    /** Envía el documento a la impresora real solo si la impresión está activada en su
     * configuración — si no, no falla ni bloquea nada, solo no imprime de verdad. En
     * ambos casos arma la vista previa a partir de las mismas líneas, para desarrollo
     * sin impresora o para revisar el diseño sin gastar papel. */
    private TicketPreviewResponse finish(String title, EscPosDocument doc) {
        boolean printed = false;
        if (printerSettingService.isPrintingEnabled()) {
            ticketPrinter.print(doc.toBytes());
            printed = true;
        }

        var lines = doc.lines().stream()
                .map(l -> new TicketLineResponse(l.text(), l.bold(), l.center(), l.rule()))
                .toList();
        return new TicketPreviewResponse(title, printed, lines);
    }

    /** Sopas, luego principios, proteínas y acompañantes, el resto al final — por el
     * rol que la selección realmente cumple (un huevo "por principio" imprime junto a
     * los principios, no donde caería su categoría cruda de catálogo). */
    private List<OrderItemSelection> sortedByPrintOrder(OrderItem item) {
        return item.getOrderItemSelections().stream()
                .sorted(Comparator.comparingInt(
                        s -> PRINT_ORDER.getOrDefault(pricingCalculator.effectiveCategoryFor(s), 99)))
                .toList();
    }

    private Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + orderId));
    }

    private String tableLabel(Order order) {
        return order.getDiningTable() != null
                ? "Mesa " + order.getDiningTable().getNumber()
                : "Para llevar";
    }

    private String money(com.miresta.shared.Money money) {
        return "$" + (money != null ? money.amount() : 0L);
    }
}
