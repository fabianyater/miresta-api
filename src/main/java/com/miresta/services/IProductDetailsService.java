package com.miresta.services;

import com.miresta.dto.ProductDetailsDto;
import com.miresta.entity.ProductDetails;

public interface IProductDetailsService {
    void updateProductDetails(ProductDetails productDetails);
    void createProductDetails(ProductDetails productDetails);
}
