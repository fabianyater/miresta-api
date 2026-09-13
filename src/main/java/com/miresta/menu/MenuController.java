package com.miresta.menu;

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
    @PreAuthorize("@access.has('MENU_EDITAR')")
    public ResponseEntity<Void> createMenu(@RequestBody CreateMenuRequest menuRequest) {
        menuService.createMenu(menuRequest);

        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("@access.has('MENU_VER')")
    public ResponseEntity<List<MenuResponse>> getMenus(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        return ResponseEntity.ok(menuService.getMenuInfo(date));
    }

    @DeleteMapping
    @PreAuthorize("@access.has('MENU_EDITAR')")
    public ResponseEntity<Void> deleteMenu(
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam("foodType") String foodType) {

        menuService.deleteMenu(date, foodType);

        return ResponseEntity.noContent().build();
    }
}
