package com.miresta.menu;

import com.miresta.catalog.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "menu_item")
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_offering_id", nullable = false)
    private MenuOffering menuOffering;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** How much is left to sell today — this is what consume()/restore() adjust, and
     * what the app shows as "Quedan N". Null means no limit was ever set. */
    @Column(name = "quantity")
    private Integer quantity;

    /** The cap originally configured for today (independent of quantity, which
     * changes as orders come in) — lets replaceMenuItems tell "the admin changed the
     * cap" apart from "this is just how much is left", so re-saving the menu can
     * recompute quantity from what's actually been consumed instead of resetting it. */
    @Column(name = "initial_quantity")
    private Integer initialQuantity;
}
