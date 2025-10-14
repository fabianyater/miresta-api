package com.miresta.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "menu_service")
public class MenuService {
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

    @OneToMany(mappedBy = "menuService")
    private Set<MenuItem> menuItems = new LinkedHashSet<>();

    @OneToMany(mappedBy = "menuService")
    private Set<OrderItem> orderItems = new LinkedHashSet<>();

}