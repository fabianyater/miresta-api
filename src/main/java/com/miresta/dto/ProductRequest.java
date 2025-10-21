package com.miresta.dto;

import java.time.LocalDate;

public record ProductRequest(String name, Long categoryId, LocalDate expirationDate, Integer quantity) {
}
