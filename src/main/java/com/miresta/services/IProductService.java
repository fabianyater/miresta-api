package com.miresta.services;

import com.miresta.entity.Product;
import com.miresta.repository.projections.ProductInfo;

import java.util.List;

public interface IProductService {
    List<ProductInfo> getProducts();
    List<ProductInfo> getProductsByCategory();
    Product getProductById(Long productId);
}
