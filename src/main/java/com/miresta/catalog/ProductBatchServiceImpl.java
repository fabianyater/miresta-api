package com.miresta.catalog;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ProductBatchServiceImpl implements IProductBatchService {
    private final ProductBatchRepository productBatchRepository;
    private final ProductServiceImpl productService;

    @Transactional
    @Override
    public ProductBatchResponse addBatch(Long productId, ProductBatchRequest request) {
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new IllegalArgumentException("La cantidad del lote debe ser mayor a 0.");
        }
        Product product = productService.getProductById(productId);

        ProductBatch batch = new ProductBatch();
        batch.setProduct(product);
        batch.setQuantityReceived(request.quantity());
        batch.setQuantityRemaining(request.quantity());
        batch.setExpirationDate(request.expirationDate());

        return toResponse(productBatchRepository.save(batch));
    }

    @Override
    public List<ProductBatchResponse> getBatches(Long productId) {
        return productBatchRepository.findByProductIdOrderByReceivedAtDescIdDesc(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @Override
    public void deleteBatch(Long batchId) {
        if (!productBatchRepository.existsById(batchId)) {
            throw new EntityNotFoundException("Lote no encontrado: " + batchId);
        }
        productBatchRepository.deleteById(batchId);
    }

    @Override
    public Map<Long, Long> getRemainingByProduct() {
        return productBatchRepository.sumRemainingGroupedByProduct().stream()
                .collect(Collectors.toMap(ProductRemainingProjection::getProductId, ProductRemainingProjection::getRemaining));
    }

    @Override
    public Map<Long, LocalDate> getNearestExpirationByProduct() {
        return productBatchRepository.nearestExpirationGroupedByProduct().stream()
                .collect(Collectors.toMap(
                        ProductNearestExpirationProjection::getProductId,
                        ProductNearestExpirationProjection::getExpirationDate));
    }

    @Transactional
    @Override
    public void consume(Product product, long amount) {
        List<ProductBatch> batches = productBatchRepository.findForConsumptionOrdered(product.getId());
        if (batches.isEmpty()) {
            return; // Sin lotes registrados para este producto — no se rastrea, no bloquea.
        }

        long remaining = amount;
        for (ProductBatch batch : batches) {
            if (remaining <= 0) break;
            long take = Math.min(batch.getQuantityRemaining(), remaining);
            if (take <= 0) continue;
            batch.setQuantityRemaining((int) (batch.getQuantityRemaining() - take));
            remaining -= take;
        }

        if (remaining > 0) {
            throw new IllegalStateException("No queda suficiente " + product.getName() + " en los lotes registrados.");
        }
        productBatchRepository.saveAll(batches);
    }

    @Transactional
    @Override
    public void restore(Product product, long amount) {
        List<ProductBatch> batches = productBatchRepository.findForConsumptionOrdered(product.getId());
        if (batches.isEmpty()) {
            return;
        }

        long remaining = amount;
        for (ProductBatch batch : batches) {
            if (remaining <= 0) break;
            long capacity = batch.getQuantityReceived() - batch.getQuantityRemaining();
            if (capacity <= 0) continue;
            long give = Math.min(capacity, remaining);
            batch.setQuantityRemaining((int) (batch.getQuantityRemaining() + give));
            remaining -= give;
        }
        // Si sobra (se restauró más de lo que cualquier lote alcanzó a absorber — no
        // debería pasar con consume/restore bien emparejados), se lo damos al primero
        // en vez de perderlo.
        if (remaining > 0) {
            ProductBatch first = batches.get(0);
            first.setQuantityRemaining((int) (first.getQuantityRemaining() + remaining));
        }
        productBatchRepository.saveAll(batches);
    }

    private ProductBatchResponse toResponse(ProductBatch b) {
        return new ProductBatchResponse(
                b.getId(), b.getQuantityReceived(), b.getQuantityRemaining(), b.getExpirationDate(), b.getReceivedAt());
    }
}
