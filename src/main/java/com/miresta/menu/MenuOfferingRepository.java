package com.miresta.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuOfferingRepository extends JpaRepository<MenuOffering, Long> {
    List<MenuOffering> findAllByMenu_Id(Long menuId);

    Optional<MenuOffering> findByMenu_IdAndFoodType_Name(Long menuId, String foodTypeName);
}
