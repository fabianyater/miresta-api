package com.miresta.order.pricing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PriceSettingRepository extends JpaRepository<PriceSetting, Long> {
    Optional<PriceSetting> findByCode(PriceCode code);
}
