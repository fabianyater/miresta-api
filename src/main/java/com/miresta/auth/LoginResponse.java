package com.miresta.auth;

public record LoginResponse(String token, String email, Role role) {
}
