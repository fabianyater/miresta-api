package com.miresta.table;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** La posición de una mesa dentro de un plano guardado. */
@Getter
@Setter
@Entity
@Table(name = "salon_layout_positions")
public class SalonLayoutPosition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "layout_id", nullable = false)
    private SalonLayout layout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private DiningTable table;

    @Column(name = "position_x", nullable = false)
    private float positionX;

    @Column(name = "position_y", nullable = false)
    private float positionY;
}
