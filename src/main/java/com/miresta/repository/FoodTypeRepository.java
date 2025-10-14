package com.miresta.repository;

import com.miresta.entity.FoodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface FoodTypeRepository extends JpaRepository<FoodType, Long> {
    Optional<FoodType> findFoodTypeByName(String name);
}