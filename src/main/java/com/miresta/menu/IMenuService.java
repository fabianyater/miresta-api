package com.miresta.menu;

import java.time.LocalDate;
import java.util.List;

public interface IMenuService {
    List<MenuResponse> getMenuInfo(LocalDate date);

    void createMenu(CreateMenuRequest request);

    /** Deletes the menu for one (date, foodType). If it was the only offering left for
     * that date, the underlying Menu row is deleted too. Refuses if any order already
     * references this menu's offering — editing it is the safe alternative. */
    void deleteMenu(LocalDate date, String foodType);
}
