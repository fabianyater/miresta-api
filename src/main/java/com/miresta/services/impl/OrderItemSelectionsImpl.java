package com.miresta.services.impl;

import com.miresta.dto.ProductWIthIdAndQuantity;
import com.miresta.entity.Category;
import com.miresta.entity.OrderItem;
import com.miresta.entity.OrderItemSelection;
import com.miresta.entity.Product;
import com.miresta.repository.CategoryRepository;
import com.miresta.repository.OrderItemSelectionsRepository;
import com.miresta.services.IOrderItemSelectionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class OrderItemSelectionsImpl implements IOrderItemSelectionsService {
    private final OrderItemSelectionsRepository orderItemSelectionsRepository;
    private final ProductServiceImpl productService;
    private final CategoryRepository categoryRepository;

    @Override
    public List<OrderItemSelection> getOrderItemSelectionByOrderItem(OrderItem orderItem) {
        return orderItemSelectionsRepository.findOrderItemSelectionByOrderItem(orderItem);
    }

    @Override
    public void createOrderItemSelection(OrderItem orderItem, List<ProductWIthIdAndQuantity> item) {
        for (var it : item) {
            Product product = productService.getProductById(it.id());
            Category category = product.getCategory();

            if (category == null) {
                throw new RuntimeException("Product has no category assigned: " + product.getId());
            }

            OrderItemSelection orderItemSelection = new OrderItemSelection();

            orderItemSelection.setOrderItem(orderItem);
            orderItemSelection.setProduct(product);
            orderItemSelection.setQuantity(it.quantity());
            orderItemSelection.setUnitExtraPrice(resolveUnitExtraPrice(orderItemSelection, category, product));

            orderItemSelectionsRepository.save(orderItemSelection);
        }
    }

    private Long resolveUnitExtraPrice(OrderItemSelection sel, Category category, Product product) {
        Long qty = sel.getQuantity();
        if (qty == null || qty <= 1) return 0L;

        String cat = normalize(category.getName());
        String name = normalize(product.getName());

        switch (cat) {
            case "sopa":
                return 5000L;

            case "principios", "adicionales", "envase":
                return 1000L;

            case "proteinas":
                return 4000L;

            case "acompanantes":
                if (name.contains("maduro")) return 0L;
                if (name.contains("ensalada")) return 1000L;
                if (name.contains("arroz")) return 1000L;

                return 1000L;

            case "especiales":
                return 10000L;

            case "bebidas":
                if (name.equals("coca-cola-1.5")) return 7000L;
                if (name.contains("personal")) return 3000L;

                return 6000L;

            default:
                return 0L;
        }
    }

    private static String normalize(String s) {
        if (s == null) return "";

        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        n = n.replace('ñ', 'n').replace('Ñ', 'N');

        return n.toLowerCase().trim();
    }
}
