package com.miresta.controller;

import com.miresta.dto.CreateMenuRequest;
import com.miresta.dto.MenuResponse;
import com.miresta.services.impl.MenuServiceImpl;
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
class MenuController {
    private final MenuServiceImpl menuService;

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
