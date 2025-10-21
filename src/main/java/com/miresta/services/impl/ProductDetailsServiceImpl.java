package com.miresta.services.impl;

import com.miresta.entity.ProductDetails;
import com.miresta.repository.ProductDetailsRepository;
import com.miresta.services.IProductDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ProductDetailsServiceImpl implements IProductDetailsService {
    private final ProductDetailsRepository productDetailsRepository;

    @Override
    public void updateProductDetails(ProductDetails productDetails) {
        productDetailsRepository.save(productDetails);
    }

    @Override
    public void createProductDetails(ProductDetails productDetails) {
        productDetailsRepository.save(productDetails);
    }
}
