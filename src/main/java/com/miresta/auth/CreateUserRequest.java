package com.miresta.auth;

public record CreateUserRequest(String email, String password, Role role) {
}
