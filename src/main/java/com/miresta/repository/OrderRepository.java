package com.miresta.repository;

import com.miresta.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByDiningTable_IdAndOrderStatus_Name(Long tableId, String statusName);

    Optional<Order> findByDiningTable_IdAndDiningTable_Status_Name(Long id, String name);
}
