package com.miresta.order.pricing;

import com.miresta.shared.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "price_settings")
public class PriceSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, unique = true)
    private PriceCode code;

    @Column(name = "label", nullable = false)
    private String label;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false))
    private Money amount;

    /**
     * False for the PROVISIONAL prices carried over from a previous project that the
     * business hasn't confirmed yet.
     */
    @Column(name = "confirmed", nullable = false)
    private boolean confirmed;
}
