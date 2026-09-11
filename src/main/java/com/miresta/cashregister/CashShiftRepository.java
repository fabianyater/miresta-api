package com.miresta.cashregister;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CashShiftRepository extends JpaRepository<CashShift, Long> {
    Optional<CashShift> findByClosedAtIsNull();

    List<CashShift> findAllByOrderByOpenedAtDesc();
}
