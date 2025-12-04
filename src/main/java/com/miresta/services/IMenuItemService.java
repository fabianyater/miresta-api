package com.miresta.services;

import com.miresta.dto.response.ProductWIthIdAndQuantity;
import com.miresta.entity.MenuService;

public interface IMenuItemService {
    void createMenuItem(MenuService menuService, ProductWIthIdAndQuantity productWIthIdAndQuantity);
}
