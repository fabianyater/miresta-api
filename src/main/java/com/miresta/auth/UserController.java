package com.miresta.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("@access.has('USUARIOS_EDITAR')")
class UserController {
    private final IUserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @RequestBody CreateUserRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUser(request, authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("@access.has('USUARIOS_VER')")
    public ResponseEntity<List<UserResponse>> getUsers() {
        return ResponseEntity.ok(userService.getUsers());
    }

    // Cualquier usuario autenticado (sin importar rol/permiso) puede editar su propia
    // info básica — correo, rol y estado activo quedan intocables desde aquí, eso
    // sigue exigiendo USUARIOS_EDITAR vía el endpoint de abajo.
    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateOwnProfile(
            @RequestBody UpdateOwnProfileRequest request, Authentication authentication) {
        return ResponseEntity.ok(userService.updateOwnProfile(request, authentication.getName()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id, @RequestBody UpdateUserRequest request, Authentication authentication) {
        return ResponseEntity.ok(userService.updateUser(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        userService.deleteUser(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
