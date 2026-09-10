package com.miresta.auth;

public record LoginResponse(String token, String email, String name, String displayName, Role role) {
}
