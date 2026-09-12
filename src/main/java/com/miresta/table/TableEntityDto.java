package com.miresta.table;

import java.io.Serializable;

/**
 * DTO for {@link DiningTable}
 */
public record TableEntityDto(
        Long id,
        Long number,
        String status,
        Long salonId,
        String salonName,
        float positionX,
        float positionY) implements Serializable {
}
