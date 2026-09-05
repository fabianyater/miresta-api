package com.miresta.order;

import com.miresta.catalog.Product;
import com.miresta.catalog.ProductServiceImpl;
import com.miresta.catalog.ProductWithIdAndQuantity;
import com.miresta.menu.MenuItemServiceImpl;
import com.miresta.order.pricing.PricingCalculator;
import com.miresta.shared.ComboCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class OrderItemSelectionsImpl implements IOrderItemSelectionsService {
    private final OrderItemSelectionsRepository orderItemSelectionsRepository;
    private final ProductServiceImpl productService;
    private final PricingCalculator pricingCalculator;
    private final MenuItemServiceImpl menuItemService;

    @Override
    public List<OrderItemSelection> getOrderItemSelectionByOrderItem(OrderItem orderItem) {
        return orderItemSelectionsRepository.findOrderItemSelectionByOrderItem(orderItem);
    }

    @Override
    public void createOrderItemSelection(OrderItem orderItem, List<ProductWithIdAndQuantity> items) {
        String mealType = orderItem.getMenuOffering().getFoodType().getName();

        for (var it : items) {
            Product product = productService.getProductById(it.id());

            if (product.getCategory() == null) {
                throw new RuntimeException("Product has no category assigned: " + product.getId());
            }

            // No-ops for anything not tracked (not on today's menu, or no quantity limit
            // set) — throws if a limit was set and there isn't enough left, which rolls
            // back the whole order (createOrder is @Transactional).
            menuItemService.consume(orderItem.getMenuOffering(), product, it.quantity());

            OrderItemSelection selection = new OrderItemSelection();
            selection.setOrderItem(orderItem);
            selection.setProduct(product);
            selection.setQuantity(it.quantity());
            selection.setReplacementCategory(parseReplacement(it.replacement()));
            selection.setUnitExtraPrice(pricingCalculator.unitExtraPriceFor(selection, mealType));

            orderItemSelectionsRepository.save(selection);
        }
    }

    private ComboCategory parseReplacement(String replacement) {
        if (replacement == null || replacement.isBlank()) {
            return null;
        }
        return ComboCategory.valueOf(replacement.trim().toUpperCase());
    }
}
