package com.miresta.order;

import com.miresta.catalog.ProductWithIdAndQuantity;

import java.util.List;

public record OrderRequest(List<ProductWithIdAndQuantity> items,
                           String mealType,
                           Long menuId,
                           Boolean isToGo,
                           Integer count,
                           String comments,
                           // Optional — this plato's own customer. Absent falls back to the
                           // request's top-level customerId (CreateOrderRequest), which keeps
                           // the common single-customer case working unchanged.
                           Long customerId) {
}
