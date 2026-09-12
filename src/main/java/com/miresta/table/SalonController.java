package com.miresta.table;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/salons")
@RequiredArgsConstructor
class SalonController {
    private final ISalonService salonService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MESERO','OWNER')")
    public ResponseEntity<List<SalonResponse>> getSalons() {
        return ResponseEntity.ok(salonService.getSalons());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<SalonResponse> createSalon(@RequestBody SalonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salonService.createSalon(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<SalonResponse> renameSalon(@PathVariable Long id, @RequestBody SalonRequest request) {
        return ResponseEntity.ok(salonService.renameSalon(id, request));
    }

    @PatchMapping("/{id}/move")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<SalonResponse> moveSalon(@PathVariable Long id, @RequestBody SalonMoveRequest request) {
        return ResponseEntity.ok(salonService.moveSalon(id, request.direction()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<Void> deleteSalon(@PathVariable Long id) {
        salonService.deleteSalon(id);
        return ResponseEntity.noContent().build();
    }
}
