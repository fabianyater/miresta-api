package com.miresta.menu;

import com.miresta.catalog.Product;
import com.miresta.catalog.ProductWithIdAndQuantity;

import java.util.List;

public interface IMenuItemService {
    void createMenuItem(MenuOffering menuOffering, ProductWithIdAndQuantity productWithIdAndQuantity);

    /** Wipes and rebuilds this offering's items from the submitted list — the create
     * form always sends the full, current selection, so this is what makes "create the
     * menu again" actually mean "edit the menu" instead of piling up duplicates. */
    void replaceMenuItems(MenuOffering menuOffering, List<ProductWithIdAndQuantity> products);

    /** No-ops if this product isn't part of the offering, or has no quantity limit set
     * (untracked = unlimited). Throws if a limit is set but there isn't enough left. */
    void consume(MenuOffering menuOffering, Product product, long amount);

    /** Undoes consume() — called when an order that already reserved stock is cancelled. */
    void restore(MenuOffering menuOffering, Product product, long amount);
}
