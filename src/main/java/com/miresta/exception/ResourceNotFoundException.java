package com.miresta.exception;

/**
 * Exception thrown when a requested resource cannot be found in the system.
 * Results in a 404 Not Found HTTP response when handled by {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
