package com.miresta.auth;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;

    public RolePermissionsResponse getMatrix() {
        List<PermissionResponse> catalog = Arrays.stream(Permission.values())
                .map(PermissionResponse::of)
                .toList();

        Map<Role, List<Permission>> rolePermissions = new EnumMap<>(Role.class);
        for (Role role : List.of(Role.ADMIN, Role.MESERO)) {
            rolePermissions.put(role, rolePermissionRepository.findByRole(role).stream()
                    .map(RolePermission::getPermission)
                    .toList());
        }

        return new RolePermissionsResponse(catalog, rolePermissions);
    }

    @Transactional
    public List<Permission> replace(Role role, List<Permission> permissions) {
        if (role == Role.OWNER) {
            throw new EntityNotFoundException("El rol OWNER siempre tiene todos los permisos y no se puede editar.");
        }
        rolePermissionRepository.deleteByRole(role);
        rolePermissionRepository.saveAll(permissions.stream()
                .distinct()
                .map(permission -> new RolePermission(role, permission))
                .toList());
        return permissions;
    }

    public List<Permission> effectivePermissions(Role role) {
        if (role == Role.OWNER) {
            return List.of(Permission.values());
        }
        return rolePermissionRepository.findByRole(role).stream()
                .map(RolePermission::getPermission)
                .toList();
    }
}
