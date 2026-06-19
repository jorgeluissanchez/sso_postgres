package com.co.eurekatic.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * CORS allowlist configuration. Bound to the {@code sso.cors.allowed-origins}
 * property as a comma-separated list (Spring Boot's standard list binding
 * handles the split). Used by both the auth-center servlet filter chain and
 * the api-gateway WebFlux chain.
 *
 * <p><b>Security model:</b> the allowlist replaces the legacy
 * {@code Allowed-Origin-Patterns: *} shortcut that was in place while the
 * stack was dev-only. The cookie-based refresh-token flow added with the
 * admin SPA requires {@code Allow-Credentials: true}, which is incompatible
 * with a wildcard {@code Access-Control-Allow-Origin} per the CORS spec;
 * we MUST have a concrete allowlist.
 *
 * <p>Defaults are dev-only and include the Vite dev server and the
 * api-gateway itself (for the production single-origin case where the
 * SPA is served from the gateway at {@code /admin/**}). Production
 * deployments MUST override via env var or a profile-specific
 * {@code application.yml}.
 */
@ConfigurationProperties(prefix = "sso.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
    /**
     * Default for local development: the Vite dev server and the
     * api-gateway origin. In production, set
     * {@code SSO_CORS_ALLOWED_ORIGINS} to a comma-separated list of the
     * real host(s) serving the SPA.
     */
    public static CorsProperties defaults() {
        return new CorsProperties(List.of(
                "http://localhost:5173",   // Vite dev server
                "http://localhost:8080"    // api-gateway in single-origin prod
        ));
    }
}
