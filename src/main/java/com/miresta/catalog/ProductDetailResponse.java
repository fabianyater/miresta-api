package com.miresta.catalog;

import com.miresta.shared.ComboCategory;

public record ProductDetailResponse(
        Long id,
        String name,
        Long categoryId,
        String categoryName,
        ComboCategory actsAsCategory,
        Long unitPrice) {
}
