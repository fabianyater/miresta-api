package com.miresta.services.impl;

import com.miresta.entity.FoodType;
import com.miresta.repository.FoodTypeRepository;
import com.miresta.services.IFoodTypeService;
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
