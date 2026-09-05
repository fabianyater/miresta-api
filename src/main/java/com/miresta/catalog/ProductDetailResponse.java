package com.miresta.catalog;

import com.miresta.shared.ComboCategory;

import java.time.LocalDate;

public record ProductDetailResponse(
        Long id,
        String name,
        Long categoryId,
        String categoryName,
        ComboCategory actsAsCategory,
        LocalDate expirationDate,
        Integer quantity,
        Long unitPrice) {
}
