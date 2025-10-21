package com.miresta.repository.projections;

import java.util.Set;

/**
 * Projection for {@link com.miresta.entity.Product}
 */
public interface ProductWithDetails {
    Long getId();

    String getName();

    CategoryInfo getCategory();

    Set<ProductDetailsInfo> getProductDetails();
}