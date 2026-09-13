package com.miresta.printing;

import com.miresta.catalog.Product;
import com.miresta.order.AccompanimentDisplay;
import com.miresta.order.IOrderService;
import com.miresta.order.Order;
import com.miresta.order.OrderItem;
import com.miresta.order.OrderItemSelection;
import com.miresta.order.OrderPaymentRepository;
import com.miresta.order.OrderPaymentResponse;
import com.miresta.order.OrderRepository;
import com.miresta.order.pricing.PricingCalculator;
import com.miresta.shared.ComboCategory;
import com.miresta.shared.Money;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class TicketService {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String RESTAURANT_LINE_1 = "Restaurante Tradición";
    private static final String RESTAURANT_LINE_2 = "Leña y Carbón";

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

    // Nombre legible de cómo priceó el plato — espejo de COMBO_LABELS del frontend.
    private static final Map<String, String> COMBO_LABELS = Map.ofEntries(
            Map.entry("ALMUERZO_COMPLETO", "Almuerzo completo"),
            Map.entry("ALMUERZO_BANDEJA", "Bandeja"),
            Map.entry("DESAYUNO_COMPLETO", "Desayuno completo"),
            Map.entry("DESAYUNO_BANDEJA", "Bandeja"),
            Map.entry("ESPECIAL_COMPLETO", "Especial"),
            Map.entry("SOLO_SOPA", "Solo sopa"),
            Map.entry("SOLO_PROTEINA", "Solo proteína"),
            Map.entry("SOLO_ACOMPANANTE", "Solo acompañante"),
            Map.entry("SUELTOS", "Sueltos"));

    private final OrderRepository orderRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final IOrderService orderService;
    private final TicketPrinter ticketPrinter;
    private final PrinterSettingService printerSettingService;
    private final PricingCalculator pricingCalculator;

    /** Comanda de cocina — sin precios, con el mesero, la mesa y el detalle de cada plato. */
    public TicketPreviewResponse printComanda(Long orderId) {
        Order order = getOrder(orderId);

        EscPosDocument doc = new EscPosDocument().left();
        doc.title("Comanda #" + order.getId());
        doc.blankLine();
        doc.line(RESTAURANT_LINE_1);
        doc.line(RESTAURANT_LINE_2);
        doc.line(DATE_TIME_FORMAT.format(order.getCreatedAt().atZone(BOGOTA)));
        doc.line(tableLabel(order));
        doc.rule();
        doc.line(field("Mesero", nvl(order.getWaiterName())));
        if (order.getCustomer() != null) {
            doc.line(field("Cliente", order.getCustomer().getName()));
        }
        doc.rule();

        for (OrderItem item : order.getOrderItems()) {
            doc.bold(true).line(itemLabel(item).toUpperCase(Locale.ROOT)).bold(false);

            for (OrderItemSelection selection : sortedByPrintOrder(item)) {
                doc.line("  " + selection.getQuantity() + "x " + selection.getProduct().getName());
            }
            for (String missing : missingNames(item)) {
                doc.line("  Sin " + missing);
            }
            if (hasMissingPrincipio(item)) {
                doc.line("  Sin principio");
            }

            if (item.getComments() != null && !item.getComments().isBlank()) {
                doc.line("  Nota: " + item.getComments());
            }
            doc.blankLine();
        }

        doc.rule().cut();
        return finish("Comanda", doc);
    }

    /** Recibo de pago para el cliente — encabezado del restaurante, detalle con precios,
     * total y método(s) de pago (o "pendiente de pago" si aún no se ha cobrado). */
    public TicketPreviewResponse printCuenta(Long orderId) {
        Order order = getOrder(orderId);
        List<OrderPaymentResponse> payments = mapPayments(order.getId());

        EscPosDocument doc = new EscPosDocument().center();
        doc.title("RECIBO DE PAGO");
        doc.line(RESTAURANT_LINE_1);
        doc.line(RESTAURANT_LINE_2);
        doc.blankLine();

        doc.left();
        doc.row("Recibo #" + order.getId(), DATE_FORMAT.format(order.getCreatedAt().atZone(BOGOTA)));
        doc.row(tableLabel(order), HOUR_FORMAT.format(order.getCreatedAt().atZone(BOGOTA)));
        doc.line(field("Mesero", nvl(order.getWaiterName())));
        if (order.getCustomer() != null) {
            doc.line(field("Cliente", order.getCustomer().getName()));
        }
        doc.rule();
        doc.bold(true).row("Descripción", "Precio").bold(false);
        doc.rule();

        for (OrderItem item : order.getOrderItems()) {
            doc.row(itemLabel(item), money(item.getTotal()));
            for (OrderItemSelection selection : sortedByPrintOrder(item)) {
                doc.line("  " + selection.getQuantity() + "x " + selection.getProduct().getName());
            }
            for (String missing : missingNames(item)) {
                doc.line("  Sin " + missing);
            }
            if (hasMissingPrincipio(item)) {
                doc.line("  Sin principio");
            }
        }

        doc.rule();
        doc.row("Subtotal", money(order.getSubtotal()));
        doc.bold(true).row("TOTAL", money(order.getTotal())).bold(false);
        doc.rule();

        if (payments.isEmpty()) {
            doc.center().bold(true).line("PENDIENTE DE PAGO").bold(false).left();
        } else {
            for (OrderPaymentResponse payment : payments) {
                doc.row(payment.paymentTypeName(), money(Money.of(payment.amount())));
            }
        }

        doc.blankLine();
        doc.center().line("¡Gracias por su visita!");
        doc.cut();

        return finish("Recibo de pago", doc);
    }

    public TicketPreviewResponse printResumenDelDia(LocalDate date) {
        var totals = orderService.getPaymentTotals(date);

        EscPosDocument doc = new EscPosDocument().center();
        doc.title("RESUMEN DEL DÍA");
        doc.line(RESTAURANT_LINE_1);
        doc.line(date.format(DATE_FORMAT));
        doc.blankLine();
        doc.left().rule();

        long grandTotal = 0L;
        long orderCount = 0L;

        for (var row : totals) {
            doc.row(row.paymentTypeName() + " (" + row.orderCount() + ")", money(row.total()));
            grandTotal += row.total().amount();
            orderCount += row.orderCount();
        }

        doc.rule();
        doc.row("Pedidos", String.valueOf(orderCount));
        doc.bold(true).row("TOTAL", money(Money.of(grandTotal))).bold(false);
        doc.cut();

        return finish("Resumen del día", doc);
    }

    /**
     * Envía el documento a la impresora real solo si la impresión está activada en su
     * configuración — si no, no falla ni bloquea nada, solo no imprime de verdad. En
     * ambos casos arma la vista previa a partir de las mismas líneas, para desarrollo
     * sin impresora o para revisar el diseño sin gastar papel.
     */
    private TicketPreviewResponse finish(String title, EscPosDocument doc) {
        boolean printed = false;
        if (printerSettingService.isPrintingEnabled()) {
            ticketPrinter.print(doc.toBytes());
            printed = true;
        }

        var lines = doc.lines().stream()
                .map(l -> new TicketLineResponse(l.text(), l.bold(), l.center(), l.rule(), l.big()))
                .toList();
        return new TicketPreviewResponse(title, printed, lines);
    }

    /**
     * Sopas, luego principios, proteínas y acompañantes, el resto al final — por el
     * rol que la selección realmente cumple (un huevo "por principio" imprime junto a
     * los principios, no donde caería su categoría cruda de catálogo). Los
     * acompañantes en su cantidad de siempre no se listan aquí — ya vienen puestos
     * por defecto, así que no aportan nada; solo los que se doblaron ("2x Arroz")
     * salen impresos (ver AccompanimentDisplay). Los que se quitaron (o, para
     * principio, no elegidos) se imprimen aparte como "Sin X" (ver {@link #missingNames}).
     */
    private List<OrderItemSelection> sortedByPrintOrder(OrderItem item) {
        List<OrderItemSelection> toPrint = new ArrayList<>(AccompanimentDisplay.nonAccompanimentSelections(item));
        toPrint.addAll(AccompanimentDisplay.doubled(AccompanimentDisplay.accompanimentSelections(item)));
        return toPrint.stream()
                .sorted(Comparator.comparingInt(
                        s -> PRINT_ORDER.getOrDefault(pricingCalculator.effectiveCategoryFor(s), 99)))
                .toList();
    }

    /** "Sin X" para cada acompañante que se quitó — el principio, si no se eligió
     * ninguno, se avisa aparte y genérico (ver {@link #hasMissingPrincipio}), ya que
     * normalmente solo se escoge uno del menú y nombrar cada opción no tomada no
     * aporta nada. */
    private List<String> missingNames(OrderItem item) {
        return AccompanimentDisplay.missingProducts(item, ComboCategory.ACOMPANANTE).stream()
                .map(Product::getName)
                .toList();
    }

    private boolean hasMissingPrincipio(OrderItem item) {
        boolean menuHasPrincipio = item.getMenuOffering().getMenuItems().stream()
                .anyMatch(mi -> mi.getProduct().getCategory().getCode() == ComboCategory.PRINCIPIO);
        boolean selectedAny = item.getOrderItemSelections().stream()
                .anyMatch(s -> s.getProduct().getCategory().getCode() == ComboCategory.PRINCIPIO);
        return menuHasPrincipio && !selectedAny;
    }

    private String itemLabel(OrderItem item) {
        String combo = item.getComboLabel();
        if (combo != null && COMBO_LABELS.containsKey(combo)) {
            return COMBO_LABELS.get(combo);
        }
        return item.getMenuOffering().getFoodType().getName();
    }

    private List<OrderPaymentResponse> mapPayments(Long orderId) {
        return orderPaymentRepository.findByOrder_IdOrderByPaidAtAsc(orderId).stream()
                .map(p -> new OrderPaymentResponse(p.getPaymentType().getName(), p.getAmount().amount()))
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

    /** "Etiqueta   valor" con la etiqueta a ancho fijo, para alinear el bloque de datos. */
    private static String field(String label, String value) {
        return String.format("%-8s %s", label, value);
    }

    private static String nvl(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String money(Money money) {
        long amount = money != null ? money.amount() : 0L;
        return String.format(Locale.US, "$%,d", amount).replace(',', '.');
    }
}
