package com.miresta.menu;

import java.util.List;

public interface IMenuOfferingService {
    MenuOffering getMenuOfferingById(Long menuOfferingId);

    MenuOffering getMenuOfferingByMenuIdAndFoodTypeName(Long menuId, String foodTypeName);

    MenuOffering createMenuOffering(Menu menu, String foodTypeName);

    /** Reuses the existing offering for this (menu, foodType) if there already is one —
     * without this, re-submitting "create menu" for the same day/meal just piled up a
     * duplicate offering instead of letting the admin edit it. */
    MenuOffering getOrCreateMenuOffering(Menu menu, String foodTypeName);

    /** Get-or-create today's Menu and this foodType's offering under it — every OrderItem
     * needs a MenuOffering to belong to, but bebidas don't actually depend on a configured
     * menu, so ordering one when there's no menu at all yet (rather than failing, or
     * silently creating an order with no items) creates an empty offering to hang it on. */
    MenuOffering getOrCreateMenuOfferingForToday(String foodTypeName);

    List<MenuOffering> getOfferingsForMenu(Menu menu);

    void deleteMenuOffering(MenuOffering menuOffering);
}
