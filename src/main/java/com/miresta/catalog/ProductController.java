package com.miresta.catalog;

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
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<ProductRequest> getProducts(@RequestBody ProductRequest productRequest) {
        productService.createProduct(productRequest);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // Reads: mesero also needs these to take orders (see the day's menu, product names,
    // etc.) — only creating/editing/deleting the catalog stays admin-only below.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<List<ProductInfo>> getProducts() {
        return ResponseEntity.ok(productService.getProducts());
    }

    @GetMapping("/details")
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<List<ProductWithDetails>> getProductsWithDetails() {
        return ResponseEntity.ok(productService.getProductsWithDetails());
    }

    @GetMapping("/category")
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<List<ProductWithDetails>> getProductsByCategory() {
        return ResponseEntity.ok(productService.getProductsByCategory());
    }

    @GetMapping("/common")
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<List<ProductInfo>> getAdditionalCommonProducts() {
        return ResponseEntity.ok(productService.getAdditionalCommonProducts());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<ProductDetailResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductDetail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<ProductDetailResponse> updateProduct(
            @PathVariable Long id, @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
