package com.miresta.menu;

import com.miresta.order.OrderItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A menu's offering for one food type (e.g. "breakfast on 2026-08-26") — what used to
 * be the entity {@code MenuService}, renamed because it collided with the service-layer
 * naming convention ({@code IMenuServicesService}/{@code MenuServicesServiceImpl}).
 */
@Getter
@Setter
@Entity
@Table(name = "menu_offering")
public class MenuOffering {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_type_id", nullable = false)
    private FoodType foodType;

    @OneToMany(mappedBy = "menuOffering")
    private Set<MenuItem> menuItems = new LinkedHashSet<>();

    @OneToMany(mappedBy = "menuOffering")
    private Set<OrderItem> orderItems = new LinkedHashSet<>();
}
