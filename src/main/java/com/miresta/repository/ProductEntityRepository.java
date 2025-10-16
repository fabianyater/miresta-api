package com.miresta.repository;

import com.miresta.entity.Product;
import com.miresta.repository.projections.ProductInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ProductEntityRepository extends JpaRepository<Product, Long> {

    @Query("select p from Product p join p.category c")
    List<ProductInfo> findAllProductsWithCategory();

    List<ProductInfo> findAllByCategoryName(String categoryName);

    @Query("select p from Product p where p.name in ?1 and p.category.name = ?2")
    List<ProductInfo> findAllByNameIn(Collection<String> names, String categoryName);
}