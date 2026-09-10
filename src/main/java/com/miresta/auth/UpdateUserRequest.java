package com.miresta.auth;

/**
 * All fields optional — only the ones present are applied. password is only set
 * (re-hashed) when non-blank, so it can be omitted to leave it unchanged.
 */
public record UpdateUserRequest(String email, String name, String displayName, String password, Role role, Boolean active) {
}
