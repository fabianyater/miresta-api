package com.miresta.catalog;

import java.time.LocalDate;

public record ProductRequest(String name, Long categoryId, LocalDate expirationDate, Integer quantity, Long unitPrice) {
}
