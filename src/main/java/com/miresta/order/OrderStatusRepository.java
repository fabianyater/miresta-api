package com.miresta.order;

import org.springframework.data.repository.Repository;

public interface OrderStatusRepository extends Repository<OrderStatus, Long> {
    OrderStatus findByName(String name);
}
