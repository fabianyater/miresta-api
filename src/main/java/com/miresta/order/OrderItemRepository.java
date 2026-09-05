package com.miresta.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findAllByOrder(Order order);

    boolean existsByMenuOffering_Id(Long menuOfferingId);

    @Query("""
        select new com.miresta.order.MealTypeRow(oi.menuOffering.foodType.name, count(oi), coalesce(sum(oi.total.amount), 0L))
        from OrderItem oi
        where oi.order.createdAt >= :from and oi.order.createdAt < :to
          and oi.order.orderStatus.name <> 'CANCELLED'
        group by oi.menuOffering.foodType.name
    """)
    List<MealTypeRow> findMealTypeCounts(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        select new com.miresta.order.FulfillmentRow(oi.orderType.name, count(oi), coalesce(sum(oi.total.amount), 0L))
        from OrderItem oi
        where oi.order.createdAt >= :from and oi.order.createdAt < :to
          and oi.order.orderStatus.name <> 'CANCELLED'
        group by oi.orderType.name
    """)
    List<FulfillmentRow> findFulfillmentCounts(@Param("from") Instant from, @Param("to") Instant to);
}
