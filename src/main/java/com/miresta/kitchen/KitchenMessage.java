package com.miresta.kitchen;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "kitchen_messages")
public class KitchenMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "text", nullable = false)
    private String text;

    /** Nombre corto (displayName) de quien lo mandó, para mostrarlo en el feed de cocina. */
    @Column(name = "sent_by", nullable = false)
    private String sentBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
