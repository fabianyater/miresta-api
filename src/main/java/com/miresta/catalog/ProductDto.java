package com.miresta.catalog;

import java.io.Serializable;

/**
 * DTO for {@link Product}
 */
public record ProductDto(Long id, String name, Integer quantity) implements Serializable {
}
