package com.miresta.order.pricing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceSettingHistoryRepository extends JpaRepository<PriceSettingHistory, Long> {
    List<PriceSettingHistory> findByPriceSetting_CodeOrderByChangedAtDesc(PriceCode code);
}
