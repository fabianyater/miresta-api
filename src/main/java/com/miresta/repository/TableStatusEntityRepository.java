package com.miresta.repository;

import com.miresta.entity.DiningTable;
import com.miresta.entity.DiningTableStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TableStatusEntityRepository extends JpaRepository<DiningTableStatus, Long> {
}