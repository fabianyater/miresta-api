package com.miresta.repository.projections;

import java.time.LocalDate;

public interface MenuInfo {
    Long getMenuId();

    LocalDate getMenuDate();

    String getFoodType();

    String getCategory();

    String getProducts();
}