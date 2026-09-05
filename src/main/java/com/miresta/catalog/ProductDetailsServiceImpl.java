package com.miresta.catalog;

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
