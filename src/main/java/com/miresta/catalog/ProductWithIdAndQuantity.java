package com.miresta.catalog;

/**
 * A catalog line used both when building a menu offering and when selecting items
 * for an order: a product, how many, and (for orders) which combo role it replaces.
 */
public record ProductWithIdAndQuantity(Long id, Long quantity, String replacement) {
}
