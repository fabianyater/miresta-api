package com.miresta.auth;

import java.util.List;

public record UpdateRolePermissionsRequest(List<Permission> permissions) {}
