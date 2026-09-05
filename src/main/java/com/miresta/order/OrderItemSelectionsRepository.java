package com.miresta.order;

import com.miresta.shared.ComboCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OrderItemSelectionsRepository extends JpaRepository<OrderItemSelection, Long> {
    List<OrderItemSelection> findOrderItemSelectionByOrderItem(OrderItem orderItem);

    /** Total quantity of selections in one combo "role" (e.g. ADICIONAL) ordered that day. */
    @Query("""
        select coalesce(sum(s.quantity), 0L)
        from OrderItemSelection s
        where s.orderItem.order.createdAt >= :from and s.orderItem.order.createdAt < :to
          and s.orderItem.order.orderStatus.name <> 'CANCELLED'
          and s.product.category.code = :category
    """)
    Long sumSelectionQuantityByCategory(
            @Param("from") Instant from, @Param("to") Instant to, @Param("category") ComboCategory category);
}
