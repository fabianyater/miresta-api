package com.miresta.catalog;

/**
 * Projection for {@link Product}
 */
public interface ProductInfo {
    Long getId();

    String getName();

    CategoryInfo getCategory();
}
