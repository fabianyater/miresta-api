package com.miresta.menu;

import java.time.LocalDate;

public interface MenuInfo {
    Long getMenuId();

    LocalDate getMenuDate();

    String getFoodType();

    String getCategory();

    String getProducts();
}
