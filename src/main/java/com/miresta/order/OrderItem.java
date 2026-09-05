package com.miresta.order;

import com.miresta.customer.Customer;
import com.miresta.menu.MenuOffering;
import com.miresta.shared.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_offering_id", nullable = false)
    private MenuOffering menuOffering;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_type_id", nullable = false)
    private OrderType orderType;

    /**
     * Who this specific plato is for — nullable. Falls back to the order's own
     * customer when absent (see OrderServiceImpl#createOrder); tracked per-item so a
     * single table ticket with platos for different people can fiar/bill each one
     * independently instead of the whole ticket always going to one customer.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    private String comments;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "base_total"))
    private Money baseTotal;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "total"))
    private Money total;

    /** What the item priced as — ALMUERZO_COMPLETO, SOLO_SOPA, SUELTOS, etc. See PricedOrderItem. */
    @Column(name = "combo_label")
    private String comboLabel;

    // Breakdown of `total` beyond baseTotal + toGoSurcharge — persisted at pricing time
    // (not recomputed later) so a "why does this cost this much" view stays accurate
    // even if price settings change afterward. See PricedOrderItem.
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "drinks_total"))
    private Money drinksTotal;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "protein_additionals_total"))
    private Money proteinAdditionalsTotal;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "side_additionals_total"))
    private Money sideAdditionalsTotal;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "extras_total"))
    private Money extrasTotal;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "individuals_total"))
    private Money individualsTotal;

    /**
     * The "para llevar" surcharge actually applied to this item (0 if dine-in or if the
     * meal type/price rule doesn't add one). Renamed from the old, misleading
     * {@code isTogoPrice} (it held an amount, not a boolean).
     */
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "togo_surcharge"))
    private Money toGoSurcharge;

    @OneToMany(mappedBy = "orderItem")
    private Set<OrderItemSelection> orderItemSelections = new LinkedHashSet<>();
}
