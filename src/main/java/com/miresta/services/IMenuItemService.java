package com.miresta.services;

import com.miresta.dto.ProductWIthIdAndQuantity;
import com.miresta.entity.MenuService;
import com.miresta.entity.Product;

import java.util.List;

public interface IMenuItemService {
    void createMenuItem(MenuService menuService, ProductWIthIdAndQuantity productWIthIdAndQuantity);
}
