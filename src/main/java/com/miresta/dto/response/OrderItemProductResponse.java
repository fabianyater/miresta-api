package com.miresta.dto.response;

public record OrderItemProductResponse(Long id,
                                       String name,
                                       Long quantity,
                                       Long unitExtraPrice,
                                       Long basePrice,
                                       String replacementForCategory) {
}
