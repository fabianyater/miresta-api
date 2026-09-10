package com.miresta.catalog;

/** Projection for the bulk "cuánto queda de cada producto con lotes" query. */
public interface ProductRemainingProjection {
    Long getProductId();

    Long getRemaining();
}
