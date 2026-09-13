package com.miresta.table;

import com.miresta.order.Order;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "number", nullable = false)
    private Long number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private DiningTableStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    /** Posición en el plano del salón, 0-100 (porcentaje del lienzo). */
    @Column(name = "position_x", nullable = false)
    private float positionX;

    @Column(name = "position_y", nullable = false)
    private float positionY;

    /** Si no es null, esta mesa está "unida" a otra — el pedido/cuenta corre por la
     * mesa principal, esta no genera uno propio mientras dure el grupo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_into_id")
    private DiningTable mergedInto;

    @OneToMany(mappedBy = "diningTable")
    private Set<Order> orders = new LinkedHashSet<>();
}
