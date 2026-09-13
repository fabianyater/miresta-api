package com.miresta.table;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/salons")
@RequiredArgsConstructor
class SalonController {
    private final ISalonService salonService;

    @GetMapping
    @PreAuthorize("@access.has('SALONES_VER')")
    public ResponseEntity<List<SalonResponse>> getSalons() {
        return ResponseEntity.ok(salonService.getSalons());
    }

    @PostMapping
    @PreAuthorize("@access.has('SALONES_EDITAR')")
    public ResponseEntity<SalonResponse> createSalon(@RequestBody SalonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salonService.createSalon(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@access.has('SALONES_EDITAR')")
    public ResponseEntity<SalonResponse> renameSalon(@PathVariable Long id, @RequestBody SalonRequest request) {
        return ResponseEntity.ok(salonService.renameSalon(id, request));
    }

    @PatchMapping("/{id}/move")
    @PreAuthorize("@access.has('SALONES_EDITAR')")
    public ResponseEntity<SalonResponse> moveSalon(@PathVariable Long id, @RequestBody SalonMoveRequest request) {
        return ResponseEntity.ok(salonService.moveSalon(id, request.direction()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.has('SALONES_EDITAR')")
    public ResponseEntity<Void> deleteSalon(@PathVariable Long id) {
        salonService.deleteSalon(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/layouts")
    @PreAuthorize("@access.has('SALONES_EDITAR')")
    public ResponseEntity<List<SalonLayoutResponse>> getLayouts(@PathVariable Long id) {
        return ResponseEntity.ok(salonService.getLayouts(id));
    }

    @PostMapping("/{id}/layouts")
    @PreAuthorize("@access.has('SALONES_EDITAR')")
    public ResponseEntity<SalonLayoutResponse> saveLayout(
            @PathVariable Long id, @RequestBody SalonLayoutRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(salonService.saveLayout(id, request, authentication.getName()));
    }

    @PatchMapping("/{id}/layouts/{layoutId}")
    @PreAuthorize("@access.has('SALONES_EDITAR')")
    public ResponseEntity<SalonLayoutResponse> renameLayout(
            @PathVariable Long id, @PathVariable Long layoutId, @RequestBody SalonLayoutRequest request) {
        return ResponseEntity.ok(salonService.renameLayout(id, layoutId, request));
    }

    @DeleteMapping("/{id}/layouts/{layoutId}")
    @PreAuthorize("@access.has('SALONES_EDITAR')")
    public ResponseEntity<Void> deleteLayout(@PathVariable Long id, @PathVariable Long layoutId) {
        salonService.deleteLayout(id, layoutId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/layouts/{layoutId}/apply")
    @PreAuthorize("@access.has('SALONES_EDITAR')")
    public ResponseEntity<Void> applyLayout(@PathVariable Long id, @PathVariable Long layoutId) {
        salonService.applyLayout(id, layoutId);
        return ResponseEntity.noContent().build();
    }
}
