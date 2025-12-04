package com.miresta.controller;

import com.miresta.dto.response.ProductDetailsDto;
import com.miresta.dto.request.ProductRequest;
import com.miresta.repository.projections.ProductInfo;
import com.miresta.repository.projections.ProductWithDetails;
import com.miresta.services.impl.ProductServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
class ProductController {
    private final ProductServiceImpl productService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductRequest> getProducts(@RequestBody ProductRequest productRequest) {
        productService.createProduct(productRequest);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateProductDetails(@RequestBody ProductDetailsDto productDetailsDto) {
        productService.updateProductDetails(productDetailsDto);

        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductInfo>> getProducts() {
        return ResponseEntity.ok(productService.getProducts());
    }

    @GetMapping("/details")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductWithDetails>> getProductsWithDetails() {
        return ResponseEntity.ok(productService.getProductsWithDetails());
    }

    @GetMapping("/category")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductWithDetails>> getProductsByCategory() {
        return ResponseEntity.ok(productService.getProductsByCategory());
    }

    @GetMapping("/common")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductInfo>> getAdditionalCommonProducts() {
        return ResponseEntity.ok(productService.getAdditionalCommonProducts());
    }
}
