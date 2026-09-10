package com.miresta.auth;

/**
 * displayName es opcional — si llega en blanco se deriva del nombre completo (su
 * primera palabra) o, en último caso, de la parte local del correo.
 */
public record CreateUserRequest(String email, String name, String displayName, String password, Role role) {
}
