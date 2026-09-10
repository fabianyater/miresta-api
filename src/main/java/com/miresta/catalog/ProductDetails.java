package com.miresta.catalog;

import com.miresta.shared.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Precio de referencia/costo de un producto — puramente informativo, no lo que se
 * cobra (eso vive en price-settings, por categoría). La cantidad y el vencimiento
 * ya no viven aquí: eso lo lleva {@link ProductBatch}, con historial por lote.
 */
@Getter
@Setter
@Entity
@Table(name = "product_details")
public class ProductDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "price"))
    private Money price;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;
}
