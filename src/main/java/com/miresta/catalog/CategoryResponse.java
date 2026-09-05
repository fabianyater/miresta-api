package com.miresta.catalog;

import com.miresta.shared.ComboCategory;
import lombok.Builder;

@Builder
public record CategoryResponse(Long id, String name, ComboCategory code) {
}
