package com.miresta.controller;

import com.miresta.dto.response.TableSummaryResponse;
import com.miresta.services.ITableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/v1/tables")
@RequiredArgsConstructor
public class TableController {
    private final ITableService tableService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TableSummaryResponse> getTablesInfo() {
        return ResponseEntity.ok(tableService.getTablesInfo());
    }
}
