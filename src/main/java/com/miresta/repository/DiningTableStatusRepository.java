package com.miresta.repository;

import com.miresta.entity.DiningTableStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiningTableStatusRepository extends JpaRepository<DiningTableStatus, Long> {
    DiningTableStatus findByName(String name);
}
