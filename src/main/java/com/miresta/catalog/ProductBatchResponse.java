package com.miresta.catalog;

import java.time.LocalDate;

public record ProductBatchResponse(
        Long id,
        Integer quantityReceived,
        Integer quantityRemaining,
        LocalDate expirationDate,
        LocalDate receivedAt) {
}
