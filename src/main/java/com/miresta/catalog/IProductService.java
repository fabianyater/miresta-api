package com.miresta.catalog;

import java.util.List;

public interface IProductService {
    void createProduct(ProductRequest productRequest);

    ProductDetailResponse getProductDetail(Long productId);

    ProductDetailResponse updateProduct(Long productId, UpdateProductRequest request);

    void deleteProduct(Long productId);

    List<ProductInfo> getProducts();

    List<ProductWithDetails> getProductsWithDetails();

    List<ProductWithDetails> getProductsByCategory();

    List<ProductInfo> getAdditionalCommonProducts();

    Product getProductById(Long productId);
}
