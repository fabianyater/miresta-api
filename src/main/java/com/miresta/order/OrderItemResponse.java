package com.miresta.order;

import com.miresta.customer.CustomerResponse;
import com.miresta.menu.MenuOfferingResponse;
import com.miresta.shared.Money;

import java.util.List;

public record OrderItemResponse(
        Long id,
        String comments,
        String comboLabel,
        // This plato's own customer if it has one, else the order's own customer (see
        // OrderServiceImpl#mapOrderItem) — never null when the order itself has a
        // customer, so the frontend doesn't need to duplicate the fallback logic.
        CustomerResponse customer,
        Money baseTotal,
        Money drinksTotal,
        Money proteinAdditionalsTotal,
        Money sideAdditionalsTotal,
        Money extrasTotal,
        Money individualsTotal,
        Money toGoSurcharge,
        Money total,
        MenuOfferingResponse menuOffering,
        OrderTypeDto orderType,
        List<GroupedOrderItemResponse> itemsByCategory) {
}
