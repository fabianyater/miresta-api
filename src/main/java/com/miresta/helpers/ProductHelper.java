package com.miresta.helpers;

import com.miresta.dto.ProductDetailsDto;
import com.miresta.dto.ProductRequest;
import com.miresta.entity.Product;
import com.miresta.entity.ProductDetails;
import com.miresta.services.IProductDetailsService;
import com.miresta.services.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
public class ProductHelper {
    private final IProductDetailsService productDetailsService;
    private final IProductService productService;

    public ProductHelper(IProductDetailsService productDetailsService, IProductService productService) {
        this.productDetailsService = productDetailsService;
        this.productService = productService;
    }

    public void createProductWithDetails(ProductRequest productRequest, Product product) {
        ProductDetails productDetails = new ProductDetails();

        productDetails.setQuantity(productRequest.quantity());
        productDetails.setExpirationDate(productRequest.expirationDate());
        productDetails.setProduct(product);

        productDetailsService.createProductDetails(productDetails);
    }

    public ProductDetails updateProductDetails(ProductDetailsDto productDetailsDto) {
        Product product = productService.getProductById(productDetailsDto.product());

        ProductDetails productDetails = new ProductDetails();

        productDetails.setProduct(product);
        productDetails.setQuantity(productDetailsDto.quantity());
        productDetails.setExpirationDate(productDetailsDto.expirationDate());

        return productDetails;
    }
}
