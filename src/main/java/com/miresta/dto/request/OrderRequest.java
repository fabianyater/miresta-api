package com.miresta.dto.request;

import com.miresta.dto.response.ProductWIthIdAndQuantity;

import java.util.List;

public record OrderRequest(List<ProductWIthIdAndQuantity> items,
                           String mealType,
                           Long menuId,
                           Boolean isToGo,
                           String comments) {
}
