package com.miresta.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {
    List<OrderPayment> findByOrder_IdOrderByPaidAtAsc(Long orderId);

    List<OrderPayment> findByOrder_Customer_IdOrderByPaidAtDesc(Long customerId);

    @Query("""
        select new com.miresta.order.PaymentTotalRow(pt.name, count(distinct op.order.id), sum(op.amount.amount))
        from OrderPayment op join op.paymentType pt
        where op.paidAt >= :from and op.paidAt < :to
        group by pt.name
    """)
    List<PaymentTotalRow> findPaymentTotals(@Param("from") Instant from, @Param("to") Instant to);
}
