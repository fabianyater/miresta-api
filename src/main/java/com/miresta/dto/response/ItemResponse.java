package com.miresta.dto.response;

import java.util.List;

public record ItemResponse(String category,
                           List<ProductDto> products) {
}
