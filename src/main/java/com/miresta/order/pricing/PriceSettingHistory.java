package com.miresta.order.pricing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "price_setting_history")
public class PriceSettingHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "price_setting_id", nullable = false)
    private PriceSetting priceSetting;

    @Column(name = "previous_amount", nullable = false)
    private long previousAmount;

    @Column(name = "new_amount", nullable = false)
    private long newAmount;

    @Column(name = "previous_label", nullable = false)
    private String previousLabel;

    @Column(name = "new_label", nullable = false)
    private String newLabel;

    @Column(name = "previous_confirmed", nullable = false)
    private boolean previousConfirmed;

    @Column(name = "new_confirmed", nullable = false)
    private boolean newConfirmed;

    /** Null only for a change made with no authenticated user attached (shouldn't happen in practice). */
    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
