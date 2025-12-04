package com.miresta.dto.response;

import com.miresta.repository.projections.ProductInfo;

import java.time.LocalDate;

public record ProductDetailsResponse(ProductInfo product, LocalDate expirationDate, Integer quantity) {
}
