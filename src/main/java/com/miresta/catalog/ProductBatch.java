package com.miresta.catalog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A single received lot of a product — its own quantity and expiration date,
 * independent of any other lot of the same product. Replaces the old single
 * quantity/expirationDate pair on {@link ProductDetails}, which got silently
 * overwritten on every edit and kept no history.
 */
@Getter
@Setter
@Entity
@Table(name = "product_batches")
public class ProductBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity_received", nullable = false)
    private Integer quantityReceived;

    @Column(name = "quantity_remaining", nullable = false)
    private Integer quantityRemaining;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "received_at", nullable = false)
    private LocalDate receivedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (receivedAt == null) receivedAt = LocalDate.now();
        if (createdAt == null) createdAt = Instant.now();
    }
}
