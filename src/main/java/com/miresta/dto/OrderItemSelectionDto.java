package com.miresta.dto;

public record OrderItemSelectionDto(
        Long id,
        Long quantity,
        Long unitExtraPrice,
        String replacementForCategory,
        ProductResponse product) {
}