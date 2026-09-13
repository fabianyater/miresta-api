package com.miresta.table;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalonLayoutRepository extends JpaRepository<SalonLayout, Long> {
    List<SalonLayout> findBySalon_IdOrderBySavedAtDesc(Long salonId);

    boolean existsBySalon_IdAndNameIgnoreCase(Long salonId, String name);

    boolean existsBySalon_IdAndNameIgnoreCaseAndIdNot(Long salonId, String name, Long id);
}
