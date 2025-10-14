package com.miresta.services;

import com.miresta.entity.Menu;
import com.miresta.entity.MenuService;

public interface IMenuServicesService {
    MenuService getMenuServiceById(Long menuServiceId);
    MenuService getMenuServiceByMenuIdAndFoodTypeName(Long menuId, String foodTypeName);
    MenuService createMenuServices(Menu menuId, String foodTypeId);
}
