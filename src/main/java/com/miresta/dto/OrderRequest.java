package com.miresta.dto;

import java.util.List;

public record OrderRequest(List<ProductWIthIdAndQuantity> items,
                           String mealType,
                           Long menuId,
                           Boolean isToGo) {
}
