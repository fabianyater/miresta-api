package com.miresta.cashregister;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CashShiftMethodClosingRepository extends JpaRepository<CashShiftMethodClosing, Long> {
    List<CashShiftMethodClosing> findByShift_Id(Long shiftId);
}
