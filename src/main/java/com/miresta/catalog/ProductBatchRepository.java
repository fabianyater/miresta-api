package com.miresta.catalog;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {

    /** Para mostrar el historial — el más reciente primero, así el "lote anterior" se ve justo debajo. */
    List<ProductBatch> findByProductIdOrderByReceivedAtDescIdDesc(Long productId);

    /** Para consumir/restaurar stock: el que vence primero se descuenta primero (FIFO por
     * vencimiento), y el lock evita que dos pedidos concurrentes lean el mismo saldo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from ProductBatch b where b.product.id = :productId " +
            "order by b.expirationDate asc nulls last, b.receivedAt asc, b.id asc")
    List<ProductBatch> findForConsumptionOrdered(@Param("productId") Long productId);

    @Query("select b.product.id as productId, coalesce(sum(b.quantityRemaining), 0) as remaining " +
            "from ProductBatch b group by b.product.id")
    List<ProductRemainingProjection> sumRemainingGroupedByProduct();
}
