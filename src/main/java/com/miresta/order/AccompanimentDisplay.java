package com.miresta.order;

import com.miresta.catalog.Product;
import com.miresta.menu.MenuItem;
import com.miresta.shared.ComboCategory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Los acompañantes de un plato ya vienen puestos por defecto al tomar el pedido (ver
 * TomarPedidoPage) — así que no vale la pena repetir los que quedaron en su cantidad
 * de siempre a quien lee el pedido (mesero, cocina, cliente). Solo importa lo que se
 * salió de lo normal: los que se quitaron y los que se doblaron. Usado tanto por la
 * respuesta de la API (OrderServiceImpl) como por la comanda de cocina (TicketService)
 * — deben mostrar exactamente lo mismo. */
public final class AccompanimentDisplay {
    private AccompanimentDisplay() {
    }

    public static List<OrderItemSelection> accompanimentSelections(OrderItem item) {
        return item.getOrderItemSelections().stream()
                .filter(AccompanimentDisplay::isAccompaniment)
                .toList();
    }

    public static List<OrderItemSelection> nonAccompanimentSelections(OrderItem item) {
        return item.getOrderItemSelections().stream()
                .filter(s -> !isAccompaniment(s))
                .toList();
    }

    public static List<OrderItemSelection> doubled(List<OrderItemSelection> accompanimentSelections) {
        return accompanimentSelections.stream()
                .filter(s -> s.getQuantity() != null && s.getQuantity() > 1)
                .toList();
    }

    /** Acompañantes del menú de hoy (sin duplicar) que no quedaron seleccionados en
     * este plato — el resto del menú no importa aquí. */
    public static List<Product> missingProducts(OrderItem item, List<OrderItemSelection> accompanimentSelections) {
        Set<Long> selectedIds = accompanimentSelections.stream()
                .map(s -> s.getProduct().getId())
                .collect(Collectors.toSet());

        List<Product> missing = new ArrayList<>();
        Set<Long> added = new HashSet<>();
        for (MenuItem menuItem : item.getMenuOffering().getMenuItems()) {
            Product product = menuItem.getProduct();
            if (product.getCategory().getCode() == ComboCategory.ACOMPANANTE
                    && !selectedIds.contains(product.getId())
                    && added.add(product.getId())) {
                missing.add(product);
            }
        }
        return missing;
    }

    private static boolean isAccompaniment(OrderItemSelection selection) {
        return selection.getProduct().getCategory().getCode() == ComboCategory.ACOMPANANTE;
    }
}
