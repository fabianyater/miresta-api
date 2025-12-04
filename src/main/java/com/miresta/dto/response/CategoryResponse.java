package com.miresta.dto.response;

import lombok.Builder;

@Builder
public record CategoryResponse(Long id, String name) {
}
