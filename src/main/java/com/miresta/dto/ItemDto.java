package com.miresta.dto;

import java.util.List;

public record ItemDto(String category,
                      List<ProductDto> products) {
}
