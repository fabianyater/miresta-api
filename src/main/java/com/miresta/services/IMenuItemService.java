package com.miresta.services;

import com.miresta.dto.response.ProductWithIdAndQuantity;
import com.miresta.entity.MenuService;

public interface IMenuItemService {
    void createMenuItem(MenuService menuService, ProductWithIdAndQuantity productWIthIdAndQuantity);
}
