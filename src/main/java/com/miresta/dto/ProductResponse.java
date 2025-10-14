package com.miresta.dto;

import com.miresta.repository.projections.ProductInfo;

public record ProductResponse(Long id, String name, ProductInfo.CategoryInfo category) {
}
