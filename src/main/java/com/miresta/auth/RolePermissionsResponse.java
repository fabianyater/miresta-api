package com.miresta.auth;

import java.util.List;
import java.util.Map;

public record RolePermissionsResponse(
        List<PermissionResponse> catalog,
        Map<Role, List<Permission>> rolePermissions
) {}
