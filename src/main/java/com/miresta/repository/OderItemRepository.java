package com.miresta.repository;

import com.miresta.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OderItemRepository extends JpaRepository<OrderItem, Long> {
}
