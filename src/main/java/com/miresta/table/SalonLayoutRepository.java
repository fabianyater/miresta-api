package com.miresta.table;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalonLayoutRepository extends JpaRepository<SalonLayout, Long> {
    Optional<SalonLayout> findBySalon_Id(Long salonId);
}
