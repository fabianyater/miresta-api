package com.miresta.repository.projections;

/**
 * Projection for {@link com.miresta.entity.Product}
 */
public interface ProductInfo {
    Long getId();

    String getName();

    CategoryInfo getCategory();

    /**
     * Projection for {@link com.miresta.entity.Category}
     */
    interface CategoryInfo {
        String getName();
    }
}