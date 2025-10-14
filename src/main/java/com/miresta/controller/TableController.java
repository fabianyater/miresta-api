package com.miresta.controller;

import com.miresta.dto.TableEntityDto;
import com.miresta.dto.TableStatusCounter;
import com.miresta.services.impl.TableServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tables")
@RequiredArgsConstructor
class TableController {
    private final TableServiceImpl tableServiceImpl;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TableEntityDto>> getTables() {
        return ResponseEntity.ok(tableServiceImpl.getTables());
    }

    @GetMapping("/status-counter")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TableStatusCounter> getTableStatusCounter() {
        return ResponseEntity.ok(new TableStatusCounter(tableServiceImpl.getFreeTablesCount(), tableServiceImpl.getOccupiedTablesCount()));
    }

}
