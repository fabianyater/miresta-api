package com.miresta.repository;

import com.miresta.entity.OrderItemSelection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemSelectionsRepository extends JpaRepository<OrderItemSelection, Long> {
}
