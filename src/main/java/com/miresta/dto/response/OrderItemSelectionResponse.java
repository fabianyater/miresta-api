package com.miresta.dto.response;

public record OrderItemSelectionResponse(
        Long id,
        Long quantity,
        Long unitExtraPrice,
        String replacementForCategory,
        ProductResponse product) {
}