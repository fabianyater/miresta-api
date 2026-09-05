package com.miresta.menu;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.miresta.catalog.ProductWithIdAndQuantity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for {@link Menu}. Upserted by (date, foodType) — see MenuServiceImpl#createMenu —
 * so submitting this again for a day/meal that already has a menu edits it in place
 * (replaces its items) instead of creating a duplicate offering.
 */
public record CreateMenuRequest(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
        LocalDate date,
        String foodType,
        List<ProductWithIdAndQuantity> products) implements Serializable {

}
