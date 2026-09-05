package com.miresta.catalog;

import com.miresta.shared.ComboCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", length = Integer.MAX_VALUE)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * Catalog-level fixed replacement policy: when set, a selection of this product
     * fills this combo role even if the selection didn't declare a replacement itself
     * (e.g. a boiled egg product that always acts as PRINCIPIO by default).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "acts_as_category")
    private ComboCategory actsAsCategory;

    @OneToMany(mappedBy = "product", orphanRemoval = true)
    private Set<ProductDetails> productDetails = new LinkedHashSet<>();
}
