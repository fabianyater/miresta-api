package com.miresta.catalog;

import com.miresta.shared.ComboCategory;

import java.time.LocalDate;

/**
 * A full replace, not a partial patch — the edit form always submits every field
 * (pre-filled from {@link ProductDetailResponse}), so there's no "unset means don't
 * change" ambiguity: name/categoryId are always applied, actsAsCategory is always set
 * (null clears an existing role override), and the expiration/quantity/price trio is
 * upserted as a whole or removed entirely when all three are left blank.
 */
public record UpdateProductRequest(
        String name,
        Long categoryId,
        ComboCategory actsAsCategory,
        LocalDate expirationDate,
        Integer quantity,
        Long unitPrice) {
}
