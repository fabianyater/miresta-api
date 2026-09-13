package com.miresta.table;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/** Un plano guardado de un salón — puede haber varios por salón, cada uno con su
 * propio nombre. "Aplicar" copia las posiciones de uno de ellos a las mesas reales. */
@Getter
@Setter
@Entity
@Table(name = "salon_layouts")
public class SalonLayout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "saved_at", nullable = false)
    private Instant savedAt;

    @Column(name = "saved_by", nullable = false)
    private String savedBy;

    @OneToMany(mappedBy = "layout", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SalonLayoutPosition> positions = new LinkedHashSet<>();
}
