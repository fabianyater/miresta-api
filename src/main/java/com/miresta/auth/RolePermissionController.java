package com.miresta.auth;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;
    private final UserRepository userRepository;

    // Hardcoded a OWNER (no @access.has): esta matriz gobierna los permisos de los
    // demás roles, así que nunca debe poder auto-otorgarse por sí misma.
    @GetMapping("/api/v1/role-permissions")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<RolePermissionsResponse> getMatrix() {
        return ResponseEntity.ok(rolePermissionService.getMatrix());
    }

    @PutMapping("/api/v1/role-permissions/{role}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<Permission>> updateRolePermissions(
            @PathVariable Role role, @RequestBody UpdateRolePermissionsRequest request) {
        return ResponseEntity.ok(rolePermissionService.replace(role, request.permissions()));
    }

    @GetMapping("/api/v1/me/permissions")
    public ResponseEntity<List<Permission>> getMyPermissions(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado."));
        return ResponseEntity.ok(rolePermissionService.effectivePermissions(user.getRole()));
    }
}
