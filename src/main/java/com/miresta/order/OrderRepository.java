package com.miresta.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByDiningTable_IdAndOrderStatus_Name(Long tableId, String statusName);

    Optional<Order> findByDiningTable_IdAndDiningTable_Status_Name(Long id, String name);

    Optional<Order> findByDiningTable_Id(Long id);

    List<Order> findByOrderStatus_Name(String name);

    Optional<Order> findByDiningTable_IdAndDiningTable_Status_NameAndOrderStatus_Name(Long id, String tableStatus, String orderStatus);

    List<Order> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    List<Order> findByCustomer_IdAndOrderStatus_NameAndPaidAtIsNull(Long customerId, String orderStatusName);

    @Query("""
        select new com.miresta.order.PaymentTotalRow(pt.name, count(o), sum(o.total.amount))
        from Order o left join o.paymentType pt
        where o.paidAt is not null
          and o.paidAt >= :from and o.paidAt < :to
        group by pt.name
    """)
    List<PaymentTotalRow> findPaymentTotals(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        select new com.miresta.order.CustomerBalanceRow(c.id, c.name, count(o), sum(o.total.amount))
        from Order o join o.customer c
        where o.orderStatus.name = 'COMPLETED' and o.paidAt is null
        group by c.id, c.name
    """)
    List<CustomerBalanceRow> findCustomerBalances();

    /** Day-level counters for the daily report — scoped by createdAt (when the ticket
     * opened), not paidAt (which findPaymentTotals covers for cash-basis totals). */
    @Query("""
        select new com.miresta.order.DaySummaryRow(
            coalesce(sum(case when o.orderStatus.name <> 'CANCELLED' then 1L else 0L end), 0L),
            coalesce(sum(case when o.orderStatus.name = 'CANCELLED' then 1L else 0L end), 0L),
            coalesce(sum(case when o.orderStatus.name = 'COMPLETED' then o.total.amount else 0L end), 0L),
            count(distinct case when o.customer is not null and o.orderStatus.name <> 'CANCELLED' then o.customer.id end)
        )
        from Order o
        where o.createdAt >= :from and o.createdAt < :to
    """)
    DaySummaryRow findDaySummary(@Param("from") Instant from, @Param("to") Instant to);

    /** Orders served but still unpaid on a customer's open tab ("fiado"), for that day. */
    @Query("""
        select new com.miresta.order.OpenTabsRow(count(o), coalesce(sum(o.total.amount), 0L))
        from Order o
        where o.orderStatus.name = 'COMPLETED' and o.paidAt is null and o.customer is not null
          and o.createdAt >= :from and o.createdAt < :to
    """)
    OpenTabsRow findOpenTabsInRange(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        select o.createdAt from Order o
        where o.createdAt >= :from and o.createdAt < :to and o.orderStatus.name <> 'CANCELLED'
    """)
    List<Instant> findOrderCreatedTimesInRange(@Param("from") Instant from, @Param("to") Instant to);

    List<Order> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);
}
