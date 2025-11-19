package com.miresta.dto;

import java.time.LocalDate;

public record MenuServiceDto(
        Long id,
        LocalDate menuDate,
        String foodType) {
}
