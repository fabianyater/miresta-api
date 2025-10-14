package com.miresta.repository;

import com.miresta.entity.OrderStatus;
import org.springframework.data.repository.Repository;

public interface OrderStatusRepository extends Repository<OrderStatus, Long> {
    OrderStatus findByName(String name);
}