package com.miresta.table;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/** El plano guardado de un salón — uno solo por salón, se sobreescribe cada vez que
 * se guarda de nuevo. "Aplicar" copia estas posiciones a las mesas reales. */
@Getter
@Setter
@Entity
@Table(name = "salon_layouts")
public class SalonLayout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_id", nullable = false, unique = true)
    private Salon salon;

    @Column(name = "saved_at", nullable = false)
    private Instant savedAt;

    @Column(name = "saved_by", nullable = false)
    private String savedBy;

    @OneToMany(mappedBy = "layout", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SalonLayoutPosition> positions = new LinkedHashSet<>();
}
