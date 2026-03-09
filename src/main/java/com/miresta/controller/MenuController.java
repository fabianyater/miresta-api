package com.miresta.controller;

import com.miresta.dto.request.CreateMenuRequest;
import com.miresta.dto.response.MenuResponse;
import com.miresta.services.IMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
public class MenuController {
    private final IMenuService menuService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> createMenu(@RequestBody CreateMenuRequest menuRequest) {
        menuService.createMenu(menuRequest);

        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MenuResponse>> getMenus(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        return ResponseEntity.ok(menuService.getMenuInfo(date));
    }
}
