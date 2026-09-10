package com.miresta.order;

import com.miresta.customer.Customer;
import com.miresta.menu.MenuOffering;
import com.miresta.menu.MenuOfferingServiceImpl;
import com.miresta.order.pricing.PricedOrderItem;
import com.miresta.order.pricing.PricingCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class OrderItemServiceImpl implements IOrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final MenuOfferingServiceImpl menuOfferingService;
    private final OrderTypeRepository orderTypeRepository;
    private final OrderItemSelectionsImpl orderItemSelectionsService;
    private final PricingCalculator pricingCalculator;

    @Override
    public OrderItem createOrderItem(Order order, Long menuId, String mealType, Boolean isToGo, String comments, Customer customer) {
        String orderTypeName = Boolean.TRUE.equals(isToGo) ? "OUT" : "IN";
        OrderType orderType = orderTypeRepository.findByName(orderTypeName)
                .orElseThrow(() -> new RuntimeException("Order type not found: " + orderTypeName));

        // Sin menú configurado para este tipo de comida todavía (típicamente: alguien pide
        // solo una bebida, que no depende del menú del día) — en vez de fallar o dejar el
        // pedido sin items, se crea/usa una oferta vacía de hoy para colgar el plato ahí.
        MenuOffering menuOffering = menuId != null
                ? menuOfferingService.getMenuOfferingByMenuIdAndFoodTypeName(menuId, mealType)
                : menuOfferingService.getOrCreateMenuOfferingForToday(mealType);
        OrderItem orderItem = new OrderItem();

        orderItem.setOrder(order);
        orderItem.setMenuOffering(menuOffering);
        orderItem.setOrderType(orderType);
        orderItem.setComments(comments);
        orderItem.setCustomer(customer);

        return orderItemRepository.save(orderItem);
    }

    @Override
    public List<OrderItem> getOrderItemsByOrder(Order order) {
        return orderItemRepository.findAllByOrder(order);
    }

    @Override
    public void updateTotalPrice(OrderItem orderItem) {
        List<OrderItemSelection> selections = orderItemSelectionsService.getOrderItemSelectionByOrderItem(orderItem);

        String mealType = orderItem.getMenuOffering().getFoodType().getName();
        boolean isToGo = "OUT".equals(orderItem.getOrderType().getName());

        PricedOrderItem priced = pricingCalculator.priceOrderItem(mealType, isToGo, selections);

        orderItem.setBaseTotal(priced.baseTotal());
        orderItem.setTotal(priced.total());
        orderItem.setToGoSurcharge(priced.toGoSurcharge());
        orderItem.setComboLabel(priced.comboLabel());
        orderItem.setDrinksTotal(priced.drinksTotal());
        orderItem.setProteinAdditionalsTotal(priced.proteinAdditionalsTotal());
        orderItem.setSideAdditionalsTotal(priced.sideAdditionalsTotal());
        orderItem.setExtrasTotal(priced.extrasTotal());
        orderItem.setIndividualsTotal(priced.individualsTotal());

        orderItemRepository.save(orderItem);
    }
}
