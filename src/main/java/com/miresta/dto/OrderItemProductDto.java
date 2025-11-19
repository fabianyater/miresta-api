package com.miresta.dto;

public record OrderItemProductDto(Long id,
                                  String name,
                                  Long quantity,
                                  Long unitExtraPrice,
                                  String replacementForCategory) {
}
