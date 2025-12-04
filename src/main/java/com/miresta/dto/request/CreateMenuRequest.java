package com.miresta.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.miresta.dto.response.ProductWIthIdAndQuantity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for {@link com.miresta.entity.Menu}
 */
public record CreateMenuRequest(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
        LocalDate date,
        String foodType,
        List<ProductWIthIdAndQuantity> products,
        String menuId) implements Serializable {

}