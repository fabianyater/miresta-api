package com.miresta.table;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalonRepository extends JpaRepository<Salon, Long> {
    List<Salon> findAllByOrderBySortOrderAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Optional<Salon> findTopByOrderBySortOrderDesc();
}
