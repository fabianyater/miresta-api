package com.miresta.auth;

public record UserResponse(Long id, String email, String name, String displayName, Role role, boolean active) {
}
