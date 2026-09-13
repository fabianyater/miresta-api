package com.miresta.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Bean SpEL "@access" usado en @PreAuthorize("@access.has('CODE')"). OWNER siempre
 * tiene acceso total y no pasa por la tabla role_permission — así nunca se puede
 * bloquear a sí mismo editando la matriz de permisos.
 */
@Component("access")
@RequiredArgsConstructor
public class AccessService {

    private final RolePermissionRepository rolePermissionRepository;

    public boolean has(String permissionCode) {
        Role role = currentRole();
        if (role == null) {
            return false;
        }
        if (role == Role.OWNER) {
            return true;
        }
        Permission permission = Permission.valueOf(permissionCode);
        return rolePermissionRepository.existsByRoleAndPermission(role, permission);
    }

    private Role currentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String name = authority.getAuthority();
            if (name.startsWith("ROLE_")) {
                try {
                    return Role.valueOf(name.substring("ROLE_".length()));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
