package com.miresta.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
class ProductBatchController {
    private final ProductBatchServiceImpl productBatchService;

    // Mesero también necesita saber cuánto queda mientras toma el pedido — solo
    // registrar/eliminar lotes queda restringido a administración, más abajo.
    @GetMapping("/api/v1/products/stock")
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<Map<Long, Long>> getStock() {
        return ResponseEntity.ok(productBatchService.getRemainingByProduct());
    }

    @GetMapping("/api/v1/products/{productId}/batches")
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<List<ProductBatchResponse>> getBatches(@PathVariable Long productId) {
        return ResponseEntity.ok(productBatchService.getBatches(productId));
    }

    @PostMapping("/api/v1/products/{productId}/batches")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<ProductBatchResponse> addBatch(
            @PathVariable Long productId, @RequestBody ProductBatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productBatchService.addBatch(productId, request));
    }

    @DeleteMapping("/api/v1/products/batches/{batchId}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Void> deleteBatch(@PathVariable Long batchId) {
        productBatchService.deleteBatch(batchId);
        return ResponseEntity.noContent().build();
    }
}
