package com.miresta.dto;

import java.io.Serializable;

/**
 * DTO for {@link com.miresta.entity.DiningTable}
 */
public record TableEntityDto(Long id, Long number, String status) implements Serializable {
}