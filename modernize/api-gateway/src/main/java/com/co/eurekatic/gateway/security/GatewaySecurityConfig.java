package com.co.eurekatic.gateway.security;

import com.co.eurekatic.common.security.CorsProperties;
import com.co.eurekatic.common.security.JwtProperties;
import com.co.eurekatic.common.security.JwtTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Reactive Spring Security 6 configuration for the gateway.
 *
 * <p>Public paths (no auth):
 * <ul>
 *   <li>{@code /actuator/health}, {@code /actuator/health/**}, {@code /actuator/info}</li>
 *   <li>{@code OPTIONS /**} (CORS preflight)</li>
 * </ul>
 *
 * <p>All other paths require a valid Bearer token, validated by
 * {@link ReactiveJwtAuthenticationFilter}. The filter runs at the
 * {@code AUTHENTICATION} order so its SecurityContext is in place
 * before Spring Security's authorization evaluation runs.
 *
 * <p>CORS is permissive in dev; tighten via the gateway's config in
 * production.
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class GatewaySecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewaySecurityConfig.class);

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            JwtTokenService jwt,
            JwtProperties jwtProperties) {

        ReactiveJwtAuthenticationFilter jwtFilter =
                new ReactiveJwtAuthenticationFilter(jwt, jwtProperties);

        return http
                .csrf(csrf -> csrf.disable())
                // CORS is handled by the auto-configured CorsWebFilter
                // created from the corsConfigurationSource() bean below.
                // Calling .cors() here would also work in theory, but
                // the inline call evaluated a fresh source at filter-
                // chain build time that doesn't reflect bean lifecycle
                // changes, leading to 403 "Invalid CORS request" on
                // every Origin-bearing request. Letting the global
                // CorsWebFilter own CORS avoids that race.
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(eh -> eh.authenticationEntryPoint(
                        new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeExchange(a -> a
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/admin", "/admin/**").permitAll()
                        // Auth flow: the user has no Bearer token at this
                        // point, so the gateway must let these through
                        // unauthenticated. auth-center enforces its own
                        // auth on business endpoints.
                        .pathMatchers("/login", "/getToken", "/getApiToken",
                                "/getInfoUser", "/getUsersSSO", "/googleLogin").permitAll()
                        .pathMatchers("/auth/refresh", "/auth/logout").permitAll()
                        // /api/** surface for the admin-ui SPA. The auth
                        // flow paths mirror the legacy ones above (the
                        // gateway routes forward them to the same
                        // auth-center endpoints — see application.yml).
                        // /api/sso-admin/** is intentionally NOT in this
                        // list; those calls carry a Bearer token and
                        // fall through to anyExchange().authenticated().
                        .pathMatchers("/auth/login").permitAll()
                        .pathMatchers("/api/auth/login").permitAll()
                        .pathMatchers("/api/auth/refresh", "/api/auth/logout").permitAll()
                        .pathMatchers("/actuator/health", "/actuator/health/**",
                                "/actuator/info").permitAll()
                        .anyExchange().authenticated())
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Defaults are dev-friendly. Production wires CorsProperties via
        // the application.yml property sso.cors.allowed-origins (list of
        // hostnames). The WebFlux security config above doesn't accept
        // a CorsProperties directly — we read it from the environment
        // here so the same allowlist applies.
        CorsProperties props = bindCorsProperties();
        log.info("CORS allowedOrigins={}", props.allowedOrigins());
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(props.allowedOrigins());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        cfg.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /**
     * Re-reads the CORS allowlist from the environment. We can't inject
     * {@link CorsProperties} directly into the bean method because
     * Spring's binding happens at startup, and we want the
     * {@code @ConfigurationProperties} bean (which {@code common} exports)
     * to drive this — we look it up by type from the application context
     * via a small helper. For now, we read the property directly via
     * {@code System.getProperty}/{@code getenv}; see
     * {@code application.yml} for the binding.
     */
    private static CorsProperties bindCorsProperties() {
        String raw = System.getProperty("sso.cors.allowed-origins",
                System.getenv().getOrDefault("SSO_CORS_ALLOWED_ORIGINS", ""));
        if (raw == null || raw.isBlank()) {
            return CorsProperties.defaults();
        }
        return new CorsProperties(
                java.util.Arrays.stream(raw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList());
    }
}
