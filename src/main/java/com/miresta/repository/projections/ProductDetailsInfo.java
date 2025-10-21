package com.miresta.repository.projections;

import java.time.LocalDate;

/**
 * Projection for {@link com.miresta.entity.ProductDetails}
 */
public interface ProductDetailsInfo {
    LocalDate getExpirationDate();

    Integer getQuantity();
}