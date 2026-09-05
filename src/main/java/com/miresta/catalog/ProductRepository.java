package com.miresta.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("select p from Product p join p.category c")
    List<ProductInfo> findAllProductsWithCategory();

    @Query("select p from Product p join p.category c left join p.productDetails pd")
    List<ProductWithDetails> findAllProductsWithDetails();

    @Query("select p from Product p join p.category c left join p.productDetails pd where p.category.name = ?1")
    List<ProductWithDetails> findAllByCategoryName(String categoryName);

    @Query("select p from Product p where p.name in ?1 and p.category.name = ?2")
    List<ProductInfo> findAllByNameIn(Collection<String> names, String categoryName);
}
