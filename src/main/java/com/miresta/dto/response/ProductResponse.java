package com.miresta.dto.response;

import com.miresta.repository.projections.CategoryInfo;

public record ProductResponse(Long id, String name, CategoryInfo category) {
}
