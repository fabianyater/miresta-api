package com.miresta.catalog;

import java.time.LocalDate;

public record ProductDetailsResponse(ProductInfo product, LocalDate expirationDate, Integer quantity) {
}
