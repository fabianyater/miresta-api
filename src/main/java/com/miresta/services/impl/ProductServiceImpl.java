package com.miresta.services.impl;

import com.miresta.entity.Product;
import com.miresta.repository.ProductEntityRepository;
import com.miresta.repository.projections.ProductInfo;
import com.miresta.services.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ProductServiceImpl implements IProductService {
    private final ProductEntityRepository productEntityRepository;

    @Override
    public List<ProductInfo> getProducts() {
        return productEntityRepository.findAllProductsWithCategory();
    }

    @Override
    public List<ProductInfo> getProductsByCategory() {
        return productEntityRepository.findAllByCategoryName("Bebidas");
    }

    @Override
    public Product getProductById(Long productId) {
        return productEntityRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
    }
}
