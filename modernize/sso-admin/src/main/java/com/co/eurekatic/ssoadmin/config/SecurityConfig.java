package com.co.eurekatic.ssoadmin.config;

import com.co.eurekatic.common.security.JwtProperties;
import com.co.eurekatic.common.security.JwtTokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 6 configuration for sso-admin.
 *
 * <p>Authorization model: every business endpoint requires
 * {@code ROLE_ADMIN}. The legacy sso-service checked the
 * username against an "admin" role inline; we keep that
 * behavior but use Spring Security's expression syntax
 * ({@code hasRole("ADMIN")}). A user with the ADMIN role on
 * their JWT can do anything; everyone else gets 403.
 *
 * <p>Public endpoints:
 * <ul>
 *   <li>{@code OPTIONS /**} — CORS preflight.</li>
 *   <li>{@code /actuator/health} and {@code /actuator/info} — health probes.</li>
 * </ul>
 *
 * <p>{@code /activateAccount} is technically public on the
 * legacy (so a user clicking the email link works without
 * being logged in). We mirror that by listing it explicitly
 * under {@code permitAll()}. Same for {@code /forgotPassword}.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtTokenService jwt,
            JwtProperties jwtProperties) throws Exception {

        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwt, jwtProperties);

        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Public — user clicks the activation link
                        // from their email and lands here without
                        // being logged in.
                        .requestMatchers("/activateAccount", "/forgotPassword").permitAll()
                        // Health probes.
                        .requestMatchers("/actuator/health", "/actuator/health/**",
                                "/actuator/info").permitAll()
                        // Everything else requires ADMIN.
                        .anyRequest().hasRole("ADMIN"))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * CORS allowlist. Permissive in dev so curl + browsers work;
     * in production the gateway enforces CORS and this stays
     * open as a defense-in-depth fallback.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("*"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        cfg.setExposedHeaders(List.of("Authorization"));
        cfg.setAllowCredentials(false);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /**
     * BCrypt with cost 12. Matches the strength used in
     * auth-center so a user created in either module can
     * authenticate against either.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * JWT parser. sso-admin only verifies tokens — auth-center
     * mints them. The key is the same on both sides
     * ({@code sso.jwt.secret}).
     */
    @Bean
    public JwtTokenService jwtTokenService(JwtProperties props) {
        return new JwtTokenService(props);
    }
}
