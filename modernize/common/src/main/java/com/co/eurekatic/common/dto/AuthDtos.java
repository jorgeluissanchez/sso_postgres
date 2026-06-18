package com.co.eurekatic.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Authentication-related DTOs. Java records; immutable, terse, and serializable
 * to JSON by Jackson out of the box.
 *
 * <p>Bean-validation annotations ({@code @NotBlank}, {@code @Size}, etc.) on
 * records are picked up by Spring controllers when {@code @Valid} is used.
 */
public final class AuthDtos {

    private AuthDtos() {
        // utility class, no instances
    }

    /**
     * Login request body. Simplified from the legacy {@code UserAuth}
     * (which also carried {@code app_name}); the modernized API-token login
     * uses a separate endpoint.
     */
    public record LoginRequest(
            @NotBlank @Size(min = 1, max = 80) String username,
            @NotBlank @Size(min = 1, max = 200) String password) {
    }

    /**
     * Successful authentication response. {@code token} is a compact JWS;
     * {@code expiresIn} is the lifetime in seconds.
     */
    public record TokenResponse(
            String token,
            String refreshToken,
            long expiresIn) {
    }

    /**
     * Lightweight public projection of the authenticated user. Never includes
     * the password hash. {@code roles} is the set of role names (not
     * "ROLE_USER"-prefixed).
     */
    public record UserSummary(
            Long id,
            String username,
            String email,
            String fullName,
            boolean enabled,
            boolean ldap,
            Set<String> roles) {
    }

    /**
     * Account creation request. Carries the minimum to create a pending
     * account; the actual password is set during activation.
     */
    public record CreateAccountRequest(
            @NotBlank @Size(min = 1, max = 80) String username,
            @NotBlank @Email @Size(max = 200) String email,
            @Size(max = 200) String fullName) {
    }
}
