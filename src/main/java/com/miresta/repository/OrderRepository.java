package com.miresta.repository;

import com.miresta.entity.Order;
import com.miresta.entity.OrderStatus;
import io.micrometer.common.KeyValues;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByDiningTable_IdAndOrderStatus_Name(Long tableId, String statusName);

    Optional<Order> findByDiningTable_IdAndDiningTable_Status_Name(Long id, String name);

    Optional<Order> findByDiningTable_Id(Long id);

    List<Order> findByOrderStatus_Name(String name);

    Optional<Order> findByDiningTable_IdAndDiningTable_Status_NameAndOrderStatus_Name(Long id, String tableStatus, String orderStatus);
}
