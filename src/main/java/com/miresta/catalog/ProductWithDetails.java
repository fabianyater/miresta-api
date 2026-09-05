package com.miresta.catalog;

import java.util.Set;

/**
 * Projection for {@link Product}
 */
public interface ProductWithDetails {
    Long getId();

    String getName();

    CategoryInfo getCategory();

    Set<ProductDetailsInfo> getProductDetails();
}
