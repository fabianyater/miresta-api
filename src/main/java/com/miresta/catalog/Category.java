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
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", length = Integer.MAX_VALUE)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "code")
    private ComboCategory code;

    @OneToMany(mappedBy = "category")
    private Set<Product> products = new LinkedHashSet<>();
}
