package com.miresta.services;

import com.miresta.dto.response.ProductDetailsDto;
import com.miresta.dto.request.ProductRequest;
import com.miresta.entity.Product;
import com.miresta.repository.projections.ProductInfo;
import com.miresta.repository.projections.ProductWithDetails;

import java.util.List;

public interface IProductService {
    void createProduct(ProductRequest productRequest);
    void updateProductDetails(ProductDetailsDto productDetailsDto);
    List<ProductInfo> getProducts();
    List<ProductWithDetails> getProductsWithDetails();
    List<ProductWithDetails> getProductsByCategory();
    List<ProductInfo> getAdditionalCommonProducts();
    Product getProductById(Long productId);
}
