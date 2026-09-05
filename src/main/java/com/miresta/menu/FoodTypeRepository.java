package com.miresta.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodTypeRepository extends JpaRepository<FoodType, Long> {
    Optional<FoodType> findFoodTypeByName(String name);
}
