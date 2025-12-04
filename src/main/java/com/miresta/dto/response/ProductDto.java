package com.miresta.dto.response;

import java.io.Serializable;

/**
 * DTO for {@link com.miresta.entity.Product}
 */
public record ProductDto(Long id, String name, Integer quantity) implements Serializable {
}