package com.miresta.menu;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class FoodTypeServiceImpl implements IFoodTypeService {
    private final FoodTypeRepository foodTypeRepository;

    @Override
    public FoodType getFoodType(String foodType) {
        return foodTypeRepository.findFoodTypeByName((foodType))
                .orElseThrow(() -> new RuntimeException("Food type not found: " + foodType));
    }
}
