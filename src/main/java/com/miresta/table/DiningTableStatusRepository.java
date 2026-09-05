package com.miresta.table;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DiningTableStatusRepository extends JpaRepository<DiningTableStatus, Long> {
    DiningTableStatus findByName(String name);
}
