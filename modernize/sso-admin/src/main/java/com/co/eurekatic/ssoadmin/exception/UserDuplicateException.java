package com.co.eurekatic.ssoadmin.exception;

/**
 * Thrown when {@code createAccount} is called with a username
 * that already exists. Mapped to HTTP 409 Conflict by
 * {@link GlobalExceptionHandler}.
 */
public class UserDuplicateException extends RuntimeException {
    public UserDuplicateException(String username) {
        super("User already exists: " + username);
    }
}
