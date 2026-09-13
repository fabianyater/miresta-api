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
        float positionY,
        /** Null si es libre o es ella misma la principal de su grupo. */
        Long mergedIntoId) implements Serializable {
}
