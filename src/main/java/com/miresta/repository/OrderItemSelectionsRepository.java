package com.miresta.repository;

import com.miresta.entity.OrderItem;
import com.miresta.entity.OrderItemSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemSelectionsRepository extends JpaRepository<OrderItemSelection, Long> {
    List<OrderItemSelection> findOrderItemSelectionByOrderItem(OrderItem orderItem);
}
