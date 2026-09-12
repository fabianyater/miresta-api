package com.miresta.table;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tables")
@RequiredArgsConstructor
class TableController {
    private final TableServiceImpl tableServiceImpl;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<TableSummaryResponse> getTablesInfo() {
        return ResponseEntity.ok(tableServiceImpl.getTablesInfo());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<TableEntityDto> createTable(@RequestBody TableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tableServiceImpl.createTable(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<TableEntityDto> renameTable(@PathVariable Long id, @RequestBody TableRequest request) {
        return ResponseEntity.ok(tableServiceImpl.renameTable(id, request));
    }

    @PatchMapping("/{id}/position")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<TableEntityDto> updatePosition(
            @PathVariable Long id, @RequestBody TablePositionRequest request) {
        return ResponseEntity.ok(tableServiceImpl.updateTablePosition(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Void> deleteTable(@PathVariable Long id) {
        tableServiceImpl.deleteTable(id);
        return ResponseEntity.noContent().build();
    }
}
