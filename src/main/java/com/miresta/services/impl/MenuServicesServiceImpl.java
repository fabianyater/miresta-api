package com.miresta.services.impl;

import com.miresta.entity.FoodType;
import com.miresta.entity.Menu;
import com.miresta.entity.MenuService;
import com.miresta.exception.ResourceNotFoundException;
import com.miresta.repository.MenuServiceRepository;
import com.miresta.services.IMenuServicesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MenuServicesServiceImpl implements IMenuServicesService {
    private final MenuServiceRepository menuServiceRepository;
    private final FoodTypeServiceImpl foodTypeService;

    @Override
    public MenuService getMenuServiceById(Long menuServiceId) {
        return menuServiceRepository.findById(menuServiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu service not found: " + menuServiceId));
    }

    @Override
    public MenuService getMenuServiceByMenuIdAndFoodTypeName(Long menuId, String foodTypeName) {
        return menuServiceRepository.findByMenu_IdAndFoodType_Name(menuId, foodTypeName.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Menu service not found for menu id: " + menuId));
    }

    @Override
    public MenuService createMenuServices(Menu menuId, String foodTypeId) {
        FoodType foodType = foodTypeService.getFoodType(foodTypeId.toUpperCase());


        MenuService menuService = new MenuService();
        menuService.setMenu(menuId);
        menuService.setFoodType(foodType);

        return menuServiceRepository.save(menuService);
    }
}
