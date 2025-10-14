package com.miresta.repository;

import com.miresta.entity.MenuService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface MenuServiceRepository extends JpaRepository<MenuService, Long> {
    Optional<MenuService> findByMenu_Id(Long id);
    Optional<MenuService> findByMenu_IdAndFoodType_Name(Long id, String name);
}