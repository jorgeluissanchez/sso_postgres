package com.co.eurekatic.auth;

import com.co.eurekatic.common.entity.Role;
import com.co.eurekatic.common.entity.User;
import com.co.eurekatic.common.repository.RoleRepository;
import com.co.eurekatic.common.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test for the auth-center module.
 *
 * <p>Boots the full Spring context on a random port (replacing the
 * Postgres datasource with an in-memory H2) and exercises the
 * authentication surface via real HTTP calls using TestRestTemplate.
 *
 * <p>Verifies the contract that downstream services will rely on:
 * <ol>
 *   <li>Bad credentials return 401.</li>
 *   <li>Good credentials return 200 + a JWT-shaped body.</li>
 *   <li>{@code /getInfoUser} accepts the token and returns the user.</li>
 *   <li>Missing token on a protected endpoint returns 401.</li>
 * </ol>
 *
 * <p>Eureka client is disabled so the test does not require a running
 * registry; we test the auth-center in isolation.
 */
@SpringBootTest(
        classes = AuthCenterApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = {
        // Replace Postgres with H2 in PostgreSQL compatibility mode
        "spring.datasource.url=jdbc:h2:mem:auth-center-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        // Keep the JWT secret stable and long enough for HS256
        "sso.jwt.secret=integration-test-secret-which-is-at-least-32-bytes-long-1234567890",
        // Don't try to register with Eureka
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class AuthCenterIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    ObjectMapper mapper;

    @BeforeEach
    void seed() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role userRole = new Role();
        userRole.setName("USER");
        roleRepository.save(userRole);

        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        roleRepository.save(adminRole);

        User alice = new User();
        alice.setUsername("alice");
        alice.setEmail("alice@example.com");
        alice.setFullName("Alice Example");
        alice.setPassword(passwordEncoder.encode("s3cret"));
        alice.setEnabled(true);
        alice.setActive(true);
        alice.setLdap(false);
        alice.addRole(userRole);
        alice.addRole(adminRole);
        userRepository.save(alice);
    }

    @Test
    void loginWithBadCredentialsReturns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = rest.exchange(
                url("/login"),
                HttpMethod.POST,
                new HttpEntity<>("{\"username\":\"alice\",\"password\":\"wrong\"}", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginWithGoodCredentialsReturnsJwt() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = rest.exchange(
                url("/login"),
                HttpMethod.POST,
                new HttpEntity<>("{\"username\":\"alice\",\"password\":\"s3cret\"}", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = mapper.readTree(response.getBody());
        assertThat(body.get("token").asText()).isNotBlank();
        assertThat(body.get("refreshToken").asText()).isNotBlank();
        assertThat(body.get("expiresIn").asLong()).isEqualTo(3600L);
    }

    @Test
    void getInfoUserAcceptsIssuedToken() throws Exception {
        String token = loginAndGetToken("alice", "s3cret");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> response = rest.exchange(
                url("/getInfoUser"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = mapper.readTree(response.getBody());
        assertThat(body.get("username").asText()).isEqualTo("alice");
        assertThat(body.get("email").asText()).isEqualTo("alice@example.com");
    }

    @Test
    void getInfoUserWithoutTokenReturns401() {
        ResponseEntity<String> response = rest.exchange(
                url("/getInfoUser"),
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void googleLoginReturns501() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = rest.exchange(
                url("/googleLogin"),
                HttpMethod.POST,
                new HttpEntity<>("{\"idToken\":\"x\",\"appName\":\"y\"}", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        JsonNode body = mapper.readTree(response.getBody());
        assertThat(body.get("error").asText()).isEqualTo("google_login_not_configured");
    }

    /* ====================== cookie / refresh / logout ====================== */

    @Test
    void loginSetsRefreshCookieWithCorrectAttributes() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = rest.exchange(
                url("/login"),
                HttpMethod.POST,
                new HttpEntity<>("{\"username\":\"alice\",\"password\":\"s3cret\"}", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        // Cookie name + scope + flags are the security guarantees the SPA
        // depends on. If any of these change, update the docs.
        assertThat(setCookie).startsWith("sso_refresh=");
        assertThat(setCookie).contains("Path=/auth");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("SameSite=Strict");
        assertThat(setCookie).contains("Max-Age=2592000"); // 30 days
        // The test runs over plain HTTP — Secure must NOT be set,
        // because if it were, the browser would never send the cookie
        // back from a non-HTTPS dev environment.
        assertThat(setCookie).doesNotContain("Secure");
    }

    @Test
    void refreshWithoutCookieReturns401() {
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<String> response = rest.exchange(
                url("/auth/refresh"),
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).contains("no_refresh_cookie");
    }

    @Test
    void refreshWithCookieReturnsNewAccessTokenAndRotatedCookie() throws Exception {
        // First, log in to get a cookie.
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> loginResp = rest.exchange(
                url("/login"), HttpMethod.POST,
                new HttpEntity<>("{\"username\":\"alice\",\"password\":\"s3cret\"}", loginHeaders),
                String.class);
        String initialCookie = loginResp.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(initialCookie).isNotNull();
        String initialValue = extractCookieValue(initialCookie);
        assertThat(initialValue).isNotBlank();

        // Now call /auth/refresh carrying the cookie.
        HttpHeaders refreshHeaders = new HttpHeaders();
        refreshHeaders.add(HttpHeaders.COOKIE, "sso_refresh=" + initialValue);
        ResponseEntity<String> refreshResp = rest.exchange(
                url("/auth/refresh"), HttpMethod.POST,
                new HttpEntity<>(refreshHeaders), String.class);

        assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = mapper.readTree(refreshResp.getBody());
        assertThat(body.get("token").asText()).isNotBlank();
        assertThat(body.get("expiresIn").asLong()).isEqualTo(3600L);

        // The Set-Cookie header is back with a NEW value (rotation).
        String newCookie = refreshResp.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(newCookie).startsWith("sso_refresh=");
        String newValue = extractCookieValue(newCookie);
        assertThat(newValue).isNotBlank();
        assertThat(newValue).isNotEqualTo(initialValue);
    }

    @Test
    void logoutSetsCookieToMaxAgeZero() {
        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<String> response = rest.exchange(
                url("/auth/logout"),
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).startsWith("sso_refresh=");
        assertThat(setCookie).contains("Max-Age=0");
        assertThat(setCookie).contains("Path=/auth");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("SameSite=Strict");
    }

    /* ====================== helpers ====================== */

    /**
     * Extracts the cookie value from a {@code Set-Cookie} header
     * (everything between {@code =} and {@code ;}). Defensive against
     * the various attribute orderings Spring may produce.
     */
    private static String extractCookieValue(String setCookie) {
        int eq = setCookie.indexOf('=');
        int semi = setCookie.indexOf(';');
        if (semi < 0) semi = setCookie.length();
        return setCookie.substring(eq + 1, semi);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = rest.exchange(
                url("/login"),
                HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(
                        Map.of("username", username, "password", password)), headers),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return mapper.readTree(response.getBody()).get("token").asText();
    }
}
