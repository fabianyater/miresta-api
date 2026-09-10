package com.miresta.catalog;

import com.miresta.shared.Money;

/**
 * Projection for {@link ProductDetails}
 */
public interface ProductDetailsInfo {
    Money getPrice();
}
