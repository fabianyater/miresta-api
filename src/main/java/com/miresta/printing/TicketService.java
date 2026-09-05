package com.miresta.printing;

import com.miresta.order.IOrderService;
import com.miresta.order.Order;
import com.miresta.order.OrderItem;
import com.miresta.order.OrderItemSelection;
import com.miresta.order.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

@RequiredArgsConstructor
@Service
public class TicketService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OrderRepository orderRepository;
    private final IOrderService orderService;
    private final TicketPrinter ticketPrinter;

    public void printComanda(Long orderId) {
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

            for (OrderItemSelection selection : item.getOrderItemSelections()) {
                doc.line("  " + selection.getQuantity() + "x " + selection.getProduct().getName());
            }

            if (item.getComments() != null && !item.getComments().isBlank()) {
                doc.line("  Nota: " + item.getComments());
            }
            doc.blankLine();
        }

        doc.cut();
        ticketPrinter.print(doc.toBytes());
    }

    public void printCuenta(Long orderId) {
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
            for (OrderItemSelection selection : item.getOrderItemSelections()) {
                doc.line("  " + selection.getQuantity() + "x " + selection.getProduct().getName());
            }
        }

        doc.rule()
                .line("Subtotal: " + money(order.getSubtotal()))
                .bold(true).line("Total: " + money(order.getTotal())).bold(false)
                .cut();

        ticketPrinter.print(doc.toBytes());
    }

    public void printResumenDelDia(LocalDate date) {
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

        ticketPrinter.print(doc.toBytes());
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
