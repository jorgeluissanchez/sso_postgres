package com.co.eurekatic.common.security;

import java.util.Set;

/**
 * Lightweight, immutable view of an authenticated principal extracted from
 * a JWT. Returned by {@link JwtTokenService#parse(String)}; consumed by
 * Spring Security's authentication machinery in the api-gateway filter.
 *
 * @param username the {@code sub} claim
 * @param roles    the {@code roles} claim (already de-prefixed; e.g. {@code "USER"})
 * @param tokenType "access" or "api"
 */
public record AuthPrincipal(String username, Set<String> roles, String tokenType) {

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
