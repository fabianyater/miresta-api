package com.miresta.catalog;

import java.util.List;
import java.util.Map;

public interface IProductBatchService {
    ProductBatchResponse addBatch(Long productId, ProductBatchRequest request);

    List<ProductBatchResponse> getBatches(Long productId);

    void deleteBatch(Long batchId);

    /** productId -> cuánto queda sumando todos sus lotes. Solo incluye productos que
     * tienen al menos un lote registrado — el resto no se rastrea así. */
    Map<Long, Long> getRemainingByProduct();

    /** No-op si el producto no tiene lotes registrados (no se rastrea por lotes).
     * Si tiene y no alcanza, lanza y deja que la transacción del pedido haga rollback. */
    void consume(Product product, long amount);

    /** Simétrico a consume — al cancelar un pedido, devuelve lo reservado. */
    void restore(Product product, long amount);
}
