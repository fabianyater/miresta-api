package com.miresta.order;

import com.miresta.catalog.ProductResponse;
import com.miresta.shared.ComboCategory;
import com.miresta.shared.Money;

public record OrderItemSelectionResponse(
        Long id,
        Long quantity,
        Money unitExtraPrice,
        ComboCategory replacementCategory,
        ProductResponse product) {
}
