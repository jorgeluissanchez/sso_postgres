package com.co.eurekatic.gateway;

import com.co.eurekatic.common.security.CorsProperties;
import com.co.eurekatic.common.security.JwtProperties;
import com.co.eurekatic.common.security.JwtTokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
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

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            JwtTokenService jwt,
            JwtProperties jwtProperties) {

        ReactiveJwtAuthenticationFilter jwtFilter =
                new ReactiveJwtAuthenticationFilter(jwt, jwtProperties);

        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .authorizeExchange(a -> a
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // /admin/** serves the SPA. The index.html and JS
                        // bundles are public; the API calls the SPA makes
                        // against /sso-admin/**, /auth/**, etc. are still
                        // authenticated by their own rules (a Bearer header
                        // or a refresh cookie). We don't want a redirect-
                        // to-login if a deep link is hit on first load —
                        // the SPA's own RequireAuth wrapper handles the
                        // unauthenticated-API case.
                        .pathMatchers("/admin", "/admin/**").permitAll()
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
