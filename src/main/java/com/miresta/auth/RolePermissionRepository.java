package com.miresta.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermission.Id> {
    boolean existsByRoleAndPermission(Role role, Permission permission);

    List<RolePermission> findByRole(Role role);

    void deleteByRole(Role role);
}
