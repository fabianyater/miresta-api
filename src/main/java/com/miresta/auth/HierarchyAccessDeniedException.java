package com.miresta.auth;

import org.springframework.security.access.AccessDeniedException;

/**
 * Thrown by our own owner/admin hierarchy checks (e.g. "solo el owner puede...") —
 * as opposed to a bare @PreAuthorize rejection, whose message is Spring Security's
 * own internal text and isn't meant to be shown to the user. SecurityConfig's
 * accessDeniedHandler uses this distinction to decide which message to surface.
 */
public class HierarchyAccessDeniedException extends AccessDeniedException {
    public HierarchyAccessDeniedException(String message) {
        super(message);
    }
}
