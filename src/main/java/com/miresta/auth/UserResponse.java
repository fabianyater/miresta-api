package com.miresta.auth;

public record UserResponse(Long id, String email, Role role, boolean active) {
}
