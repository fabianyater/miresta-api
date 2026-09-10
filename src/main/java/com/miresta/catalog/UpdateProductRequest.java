package com.miresta.catalog;

import com.miresta.shared.ComboCategory;

/**
 * A full replace, not a partial patch — the edit form always submits every field
 * (pre-filled from {@link ProductDetailResponse}): name/categoryId are always
 * applied, actsAsCategory is always set (null clears an existing role override),
 * and unitPrice is upserted or cleared when left blank. Quantity/expiration no
 * longer live here — those are lotes, managed through {@link ProductBatchController}.
 */
public record UpdateProductRequest(
        String name,
        Long categoryId,
        ComboCategory actsAsCategory,
        Long unitPrice) {
}
