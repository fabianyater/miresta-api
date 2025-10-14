package com.miresta.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "dining_table")
public class DiningTable {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "number", nullable = false)
    private Long number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private DiningTableStatus status;

    @OneToMany(mappedBy = "diningTable")
    private Set<Order> orders = new LinkedHashSet<>();

}