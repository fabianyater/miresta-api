package com.miresta.order;

import com.miresta.catalog.Product;
import com.miresta.shared.ComboCategory;
import com.miresta.shared.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "order_item_selections")
public class OrderItemSelection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "unit_extra_price"))
    private Money unitExtraPrice;

    /**
     * The combo role this selection fills instead of the product's usual one
     * (e.g. an egg selected to replace the principio). Null when the selection
     * just occupies its product's own category.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "replacement_category")
    private ComboCategory replacementCategory;
}
