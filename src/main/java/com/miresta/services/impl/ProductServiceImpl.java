package com.miresta.services.impl;

import com.miresta.dto.response.ProductDetailsDto;
import com.miresta.dto.request.ProductRequest;
import com.miresta.entity.Category;
import com.miresta.entity.Product;
import com.miresta.entity.ProductDetails;
import com.miresta.repository.ProductEntityRepository;
import com.miresta.repository.projections.ProductInfo;
import com.miresta.repository.projections.ProductWithDetails;
import com.miresta.services.IProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class ProductServiceImpl implements IProductService {
    private final ProductEntityRepository productEntityRepository;
    private final CategoryServiceImpl categoryServiceImpl;
    private final ProductDetailsServiceImpl productDetailsServiceImpl;

    @Transactional
    @Override
    public void createProduct(ProductRequest productRequest) {
        Category category = categoryServiceImpl.getCategoryById(productRequest.categoryId());
        Product product = new Product();

        product.setName(productRequest.name());
        product.setCategory(category);

        var savedProduct = productEntityRepository.save(product);

        if (productRequest.expirationDate() != null && productRequest.quantity() != null && productRequest.unitPrice() > 0) {
            ProductDetails productDetails = new ProductDetails();

            productDetails.setProduct(savedProduct);
            productDetails.setQuantity(productRequest.quantity());
            productDetails.setExpirationDate(productRequest.expirationDate());
            productDetails.setPrice(productRequest.unitPrice());

            productDetailsServiceImpl.createProductDetails(productDetails);
        }
    }

    @Override
    public void updateProductDetails(ProductDetailsDto productDetailsDto) {
        Product product = getProductById(productDetailsDto.product());

        ProductDetails productDetails = new ProductDetails();

        productDetails.setProduct(product);
        productDetails.setQuantity(productDetailsDto.quantity());
        productDetails.setExpirationDate(productDetailsDto.expirationDate());

        productDetailsServiceImpl.updateProductDetails(productDetails);
    }

    @Override
    public List<ProductInfo> getProducts() {
        return productEntityRepository.findAllProductsWithCategory();
    }

    @Override
    public List<ProductWithDetails> getProductsWithDetails() {
        return productEntityRepository.findAllProductsWithDetails().stream()
                .sorted(Comparator.comparing(o -> o.getCategory().getName()))
                .toList();
    }

    @Override
    public List<ProductWithDetails> getProductsByCategory() {
        return productEntityRepository.findAllByCategoryName("Bebidas");
    }

    @Override
    public List<ProductInfo> getAdditionalCommonProducts() {
        String[] commonProducts = {"Huevo cocido", "Huevo frito", "Huevos revueltos"};
        return productEntityRepository.findAllByNameIn(List.of(commonProducts), "Adicionales");
    }

    @Override
    public Product getProductById(Long productId) {
        return productEntityRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
    }
}
