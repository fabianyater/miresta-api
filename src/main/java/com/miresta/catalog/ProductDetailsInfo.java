package com.miresta.catalog;

import com.miresta.shared.Money;

import java.time.LocalDate;

/**
 * Projection for {@link ProductDetails}
 */
public interface ProductDetailsInfo {
    LocalDate getExpirationDate();

    Integer getQuantity();

    Money getPrice();
}
