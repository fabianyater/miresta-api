package com.miresta.dto.response;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO for {@link com.miresta.entity.ProductDetails}
 */
public record ProductDetailsDto(LocalDate expirationDate,
                                Integer quantity,
                                Long product) implements Serializable {
}