package com.miresta.repository;

import com.miresta.entity.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiningRepository extends JpaRepository<DiningTable, Long> {
}
