package com.miresta.auth;

/**
 * Igual que UpdateUserRequest pero sin email/rol/active — cualquier usuario
 * autenticado puede editar su propia info básica, pero no esos tres campos.
 */
public record UpdateOwnProfileRequest(String name, String displayName, String password) {
}
