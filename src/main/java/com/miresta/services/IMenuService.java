package com.miresta.services;

import com.miresta.dto.request.CreateMenuRequest;
import com.miresta.dto.response.MenuResponse;

import java.time.LocalDate;
import java.util.List;

public interface IMenuService {
    List<MenuResponse> getMenuInfo(LocalDate date);
    void createMenu(CreateMenuRequest request);
}
