package com.miresta.table;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/** Una sección del restaurante (ej. "Salón principal", "Terraza") — agrupa mesas y
 * define en qué orden aparecen sus pestañas. */
@Getter
@Setter
@Entity
@Table(name = "salons")
public class Salon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @OneToMany(mappedBy = "salon")
    private Set<DiningTable> tables = new LinkedHashSet<>();
}
