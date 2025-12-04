package com.miresta.dto.response;

import java.time.LocalDate;

public record MenuServiceResponse(
        Long id,
        LocalDate menuDate,
        String foodType) {
}
