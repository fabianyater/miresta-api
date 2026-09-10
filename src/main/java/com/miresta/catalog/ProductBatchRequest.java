package com.miresta.catalog;

import java.time.LocalDate;

public record ProductBatchRequest(Integer quantity, LocalDate expirationDate) {
}
