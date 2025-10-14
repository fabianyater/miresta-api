package com.miresta.services;

import com.miresta.dto.CreateMenuRequest;
import com.miresta.dto.MenuResponse;
import com.miresta.entity.MenuService;
import com.miresta.repository.projections.MenuInfo;

import java.time.LocalDate;
import java.util.List;

public interface IMenuService {
    List<MenuResponse> getMenuInfo(LocalDate date);
    void createMenu(CreateMenuRequest request);
}
