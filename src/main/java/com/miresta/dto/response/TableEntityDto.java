package com.miresta.dto.response;

import java.io.Serializable;

/**
 * DTO for {@link com.miresta.entity.DiningTable}
 */
public record TableEntityDto(Long id, Long number, String status) implements Serializable {
}