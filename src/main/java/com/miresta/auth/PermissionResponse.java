package com.miresta.auth;

public record PermissionResponse(Permission code, String domain, String description) {
    static PermissionResponse of(Permission permission) {
        return new PermissionResponse(permission, permission.domain(), permission.description());
    }
}
