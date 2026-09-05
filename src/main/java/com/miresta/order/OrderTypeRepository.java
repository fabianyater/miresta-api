package com.miresta.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderTypeRepository extends JpaRepository<OrderType, Long> {
    Optional<OrderType> findByName(String name);
}
