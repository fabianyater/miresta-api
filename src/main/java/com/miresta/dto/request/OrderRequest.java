package com.miresta.dto.request;

import com.miresta.dto.response.ProductWithIdAndQuantity;

import java.util.List;

public record OrderRequest(List<ProductWithIdAndQuantity> items,
                           String mealType,
                           Long menuId,
                           Boolean isToGo,
                           Integer count,
                           String comments) {
}
