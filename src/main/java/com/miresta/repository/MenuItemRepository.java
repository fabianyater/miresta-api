package com.miresta.repository;

import com.miresta.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.Repository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
}