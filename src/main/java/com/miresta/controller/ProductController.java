package com.miresta.controller;

import com.miresta.repository.projections.ProductInfo;
import com.miresta.services.impl.ProductServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
class ProductController {
    private final ProductServiceImpl productService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductInfo>> getProducts() {
        return ResponseEntity.ok(productService.getProducts());
    }

    @GetMapping("/category")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductInfo>> getProductsByCategory() {
        return ResponseEntity.ok(productService.getProductsByCategory());
    }
}
