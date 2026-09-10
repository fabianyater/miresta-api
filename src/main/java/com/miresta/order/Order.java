package com.miresta.order;

import com.miresta.customer.Customer;
import com.miresta.shared.Money;
import com.miresta.table.DiningTable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dining_table_id")
    private DiningTable diningTable;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_status_id", nullable = false)
    private OrderStatus orderStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_type_id")
    private PaymentType paymentType;

    @Column(name = "notes", length = Integer.MAX_VALUE)
    private String notes;

    /**
     * Null while the order is an unpaid, open tab (fiado) billed to a customer —
     * distinct from orderStatus, which tracks service/kitchen state, not payment.
     */
    @Column(name = "paid_at")
    private Instant paidAt;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "subtotal"))
    private Money subtotal;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total"))
    private Money total;

    @Column(name = "waiters")
    private String waiterName;

    @OneToMany(mappedBy = "order")
    private Set<OrderItem> orderItems = new LinkedHashSet<>();
}
