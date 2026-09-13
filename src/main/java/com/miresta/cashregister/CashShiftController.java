package com.miresta.cashregister;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cash-shifts")
@RequiredArgsConstructor
public class CashShiftController {

    private final CashShiftService service;

    @GetMapping("/current")
    @PreAuthorize("@access.has('CAJA_VER')")
    public ResponseEntity<CashShiftResponse> current() {
        return ResponseEntity.ok(service.getCurrent());
    }

    @GetMapping
    @PreAuthorize("@access.has('CAJA_VER')")
    public ResponseEntity<List<CashShiftResponse>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.has('CAJA_VER')")
    public ResponseEntity<CashShiftResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping("/open")
    @PreAuthorize("@access.has('CAJA_EDITAR')")
    public ResponseEntity<CashShiftResponse> open(
            @RequestBody OpenShiftRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.open(request.openingCash(), authentication.getName()));
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("@access.has('CAJA_EDITAR')")
    public ResponseEntity<CashShiftResponse> close(
            @PathVariable Long id, @RequestBody CloseShiftRequest request, Authentication authentication) {
        return ResponseEntity.ok(
                service.close(id, request.countedByMethod(), request.notes(), authentication.getName()));
    }

    @PostMapping("/{id}/movements")
    @PreAuthorize("@access.has('CAJA_EDITAR')")
    public ResponseEntity<CashShiftResponse> addMovement(
            @PathVariable Long id, @RequestBody CashMovementRequest request, Authentication authentication) {
        return ResponseEntity.ok(service.addMovement(
                id, request.type(), request.amount(), request.reason(), authentication.getName()));
    }
}
