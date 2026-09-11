package com.miresta.cashregister;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {
    List<CashMovement> findByShift_IdOrderByCreatedAtAsc(Long shiftId);
}
