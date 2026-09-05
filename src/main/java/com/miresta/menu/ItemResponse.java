package com.miresta.menu;

import com.miresta.catalog.ProductDto;

import java.util.List;

public record ItemResponse(String category,
                           List<ProductDto> products) {
}
