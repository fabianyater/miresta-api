package com.miresta.dto.request;

import java.time.LocalDate;

public record ProductRequest(String name, Long categoryId, LocalDate expirationDate, Integer quantity, Long unitPrice) {
}
