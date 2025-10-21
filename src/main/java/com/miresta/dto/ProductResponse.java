package com.miresta.dto;

import com.miresta.repository.projections.CategoryInfo;
import com.miresta.repository.projections.ProductInfo;

public record ProductResponse(Long id, String name, CategoryInfo category) {
}
