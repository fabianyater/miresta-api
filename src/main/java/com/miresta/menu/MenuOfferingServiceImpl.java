package com.miresta.menu;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class MenuOfferingServiceImpl implements IMenuOfferingService {
    private final MenuOfferingRepository menuOfferingRepository;
    private final FoodTypeServiceImpl foodTypeService;

    @Override
    public MenuOffering getMenuOfferingById(Long menuOfferingId) {
        return menuOfferingRepository.findById(menuOfferingId)
                .orElseThrow(() -> new EntityNotFoundException("Menu offering not found: " + menuOfferingId));
    }

    @Override
    public MenuOffering getMenuOfferingByMenuIdAndFoodTypeName(Long menuId, String foodTypeName) {
        return menuOfferingRepository.findByMenu_IdAndFoodType_Name(menuId, foodTypeName.toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No hay menú de " + foodTypeName.toLowerCase() + " configurado para esta fecha."));
    }

    @Override
    public MenuOffering createMenuOffering(Menu menu, String foodTypeName) {
        FoodType foodType = foodTypeService.getFoodType(foodTypeName.toUpperCase());

        MenuOffering menuOffering = new MenuOffering();
        menuOffering.setMenu(menu);
        menuOffering.setFoodType(foodType);

        return menuOfferingRepository.save(menuOffering);
    }

    @Override
    public MenuOffering getOrCreateMenuOffering(Menu menu, String foodTypeName) {
        return menuOfferingRepository.findByMenu_IdAndFoodType_Name(menu.getId(), foodTypeName.toUpperCase())
                .orElseGet(() -> createMenuOffering(menu, foodTypeName));
    }

    @Override
    public List<MenuOffering> getOfferingsForMenu(Menu menu) {
        return menuOfferingRepository.findAllByMenu_Id(menu.getId());
    }

    @Override
    public void deleteMenuOffering(MenuOffering menuOffering) {
        menuOfferingRepository.delete(menuOffering);
    }
}
