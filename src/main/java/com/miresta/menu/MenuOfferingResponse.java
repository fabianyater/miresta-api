package com.miresta.menu;

import java.time.LocalDate;

public record MenuOfferingResponse(
        Long id,
        LocalDate menuDate,
        String foodType) {
}
