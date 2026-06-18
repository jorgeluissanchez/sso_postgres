package com.co.eurekatic.ssoadmin;

import com.co.eurekatic.common.entity.Role;
import com.co.eurekatic.common.entity.User;
import com.co.eurekatic.common.repository.EndpointRepository;
import com.co.eurekatic.common.repository.GroupRepository;
import com.co.eurekatic.common.repository.MicroserviceRepository;
import com.co.eurekatic.common.repository.RoleRepository;
import com.co.eurekatic.common.repository.RouteRepository;
import com.co.eurekatic.common.repository.UserRepository;
import com.co.eurekatic.common.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * End-to-end integration test for the sso-admin module.
 *
 * <p>Boots the full Spring context with an in-memory H2
 * database (PostgreSQL compatibility mode + NON_KEYWORDS=GROUPS
 * so the {@code groups} table can be created), exchanges real
 * HTTP requests through TestRestTemplate, and asserts the
 * security rules, the activation flow, and the role binding
 * round-trip.
 *
 * <p>The {@link com.co.eurekatic.ssoadmin.service.EmailService}
 * bean is replaced with a mock — we don't want SMTP in tests.
 */
@SpringBootTest(
        classes = SsoAdminApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:sso-admin-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;NON_KEYWORDS=GROUPS;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "sso.jwt.secret=integration-test-secret-which-is-at-least-32-bytes-long-1234567890",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.mail.host=localhost",
        "spring.mail.port=2525",
        // Don't run mail health probe — the fake SMTP isn't
        // actually serving, and the indicator would push the
        // /actuator/health aggregate to 503.
        "management.health.mail.enabled=false"
})
class SsoAdminIntegrationTest {

    @LocalServerPort int port;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired GroupRepository groupRepository;
    @Autowired MicroserviceRepository microserviceRepository;
    @Autowired EndpointRepository endpointRepository;
    @Autowired RouteRepository routeRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenService jwtService;
    @Autowired ObjectMapper mapper;
    @Autowired TestRestTemplate rest;

    // Don't hit SMTP in tests.
    @MockBean
    com.co.eurekatic.ssoadmin.service.EmailService emailServiceMock;

    @BeforeEach
    void seed() {
        // Order matters: child tables before parents so FK
        // constraints don't trip on the H2 re-init.
        routeRepository.deleteAll();
        endpointRepository.deleteAll();
        microserviceRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        groupRepository.deleteAll();

        Role admin = new Role("ADMIN", "Administrator");
        roleRepository.save(admin);

        User root = new User();
        root.setUsername("root");
        root.setEmail("root@example.com");
        root.setFullName("Root Admin");
        root.setPassword(passwordEncoder.encode("r00t"));
        root.setEnabled(true);
        root.setActive(true);
        root.addRole(admin);
        userRepository.save(root);

        // Default no-op so the mock doesn't blow up.
        doNothing().when(emailServiceMock).sendActivationEmail(any(), any());
        doNothing().when(emailServiceMock).sendRestorePasswordEmail(any(), any());
    }

    /* ====================== public endpoints ====================== */

    @Test
    void healthEndpointIsPublic() {
        ResponseEntity<String> resp = rest.getForEntity(url("/actuator/health"), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void protectedEndpointRejectsRequestWithoutToken() {
        ResponseEntity<String> resp = rest.getForEntity(url("/getUsers"), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /* ====================== auth-gated CRUD ====================== */

    @Test
    void getUsersReturnsSeededAdmin() throws Exception {
        String token = tokenFor("root", "ADMIN");

        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        ResponseEntity<String> resp = rest.exchange(
                url("/getUsers"), HttpMethod.GET, new HttpEntity<>(h), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = mapper.readTree(resp.getBody());
        assertThat(body.isArray()).isTrue();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).get("username").asText()).isEqualTo("root");
        // The password is NEVER exposed in the response.
        assertThat(body.get(0).has("password")).isFalse();
    }

    @Test
    void createAccountPersistsUserWithDisabledFlagAndSendsEmail() throws Exception {
        String token = tokenFor("root", "ADMIN");
        String body = mapper.writeValueAsString(Map.of(
                "fullName", "Alice Example",
                "username", "alice",
                "email", "alice@example.com",
                "password", "s3cret",
                "passwordConfirm", "s3cret",
                "roleNames", List.of("ADMIN")
        ));

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        ResponseEntity<String> resp = rest.exchange(
                url("/createAccount"), HttpMethod.POST,
                new HttpEntity<>(body, h), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode json = mapper.readTree(resp.getBody());
        assertThat(json.get("username").asText()).isEqualTo("alice");
        assertThat(json.get("active").asBoolean()).isTrue();
        assertThat(json.get("ldap").asBoolean()).isFalse();

        User stored = userRepository.findByUsername("alice").orElseThrow();
        // Until the activation link is clicked, the user cannot log in.
        assertThat(stored.isEnabled()).isFalse();
        assertThat(stored.getTokenActivation()).isNotBlank();
        // Password was BCrypted, not stored as plaintext.
        assertThat(stored.getPassword()).isNotEqualTo("s3cret");
        assertThat(passwordEncoder.matches("s3cret", stored.getPassword())).isTrue();

        org.mockito.Mockito.verify(emailServiceMock)
                .sendActivationEmail(any(User.class), any());
    }

    @Test
    void createAccountRejectsDuplicateUsernameWith409() throws Exception {
        String token = tokenFor("root", "ADMIN");
        String body = mapper.writeValueAsString(Map.of(
                "fullName", "Another Root",
                "username", "root",
                "email", "other@example.com",
                "password", "s3cret"
        ));
        HttpHeaders h = jsonHeadersWith(token);

        ResponseEntity<String> resp = rest.exchange(
                url("/createAccount"), HttpMethod.POST,
                new HttpEntity<>(body, h), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(mapper.readTree(resp.getBody()).get("code").asText())
                .isEqualTo("USER_DUPLICATE");
    }

    @Test
    void createAccountRejectsInvalidEmailWith422() throws Exception {
        String token = tokenFor("root", "ADMIN");
        String body = mapper.writeValueAsString(Map.of(
                "fullName", "Bad Email",
                "username", "badmail",
                "email", "not-an-email",
                "password", "s3cret"
        ));
        HttpHeaders h = jsonHeadersWith(token);

        ResponseEntity<String> resp = rest.exchange(
                url("/createAccount"), HttpMethod.POST,
                new HttpEntity<>(body, h), String.class);

        // @Email validation kicks in before our second-line regex,
        // so the response is the generic VALIDATION_FAILED.
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void activateAccountEnablesUserWithoutAuth() throws Exception {
        // First, create a user — capture the activation token via the
        // mock EmailService argument.
        String token = tokenFor("root", "ADMIN");
        String body = mapper.writeValueAsString(Map.of(
                "fullName", "Bob", "username", "bob", "email", "bob@example.com",
                "password", "s3cret"));
        HttpHeaders h = jsonHeadersWith(token);
        rest.exchange(url("/createAccount"), HttpMethod.POST,
                new HttpEntity<>(body, h), String.class);

        User created = userRepository.findByUsername("bob").orElseThrow();
        String activationToken = created.getTokenActivation();
        assertThat(activationToken).isNotBlank();

        // No Authorization header — the link from the email does NOT include one.
        ResponseEntity<String> resp = rest.exchange(
                url("/activateAccount?token=" + activationToken + "&password=newpass1"),
                HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        User after = userRepository.findByUsername("bob").orElseThrow();
        assertThat(after.isEnabled()).isTrue();
        assertThat(after.isActive()).isTrue();
        // Token column cleared on use — cannot be replayed.
        assertThat(after.getTokenActivation()).isNull();
        assertThat(passwordEncoder.matches("newpass1", after.getPassword())).isTrue();
    }

    @Test
    void activateAccountWithUnknownTokenReturns404() {
        ResponseEntity<String> resp = rest.exchange(
                url("/activateAccount?token=does-not-exist&password=newpass1"),
                HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void roleCreateAndListRoundTrip() throws Exception {
        String token = tokenFor("root", "ADMIN");
        HttpHeaders h = jsonHeadersWith(token);

        ResponseEntity<String> created = rest.exchange(
                url("/role/createRole"), HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(
                        Map.of("name", "AUDITOR", "description", "Read only")), h),
                String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> all = rest.exchange(
                url("/role/getRoles"), HttpMethod.GET, new HttpEntity<>(h), String.class);
        assertThat(all.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode arr = mapper.readTree(all.getBody());
        assertThat(arr).hasSizeGreaterThanOrEqualTo(2); // ADMIN + AUDITOR

        ResponseEntity<String> own = rest.exchange(
                url("/role/getRolesOwn"), HttpMethod.GET, new HttpEntity<>(h), String.class);
        JsonNode ownArr = mapper.readTree(own.getBody());
        // Just make sure the endpoint shape is what the legacy UI expects.
        assertThat(ownArr.isArray()).isTrue();
    }

    @Test
    void groupCreateAndList() throws Exception {
        String token = tokenFor("root", "ADMIN");
        HttpHeaders h = jsonHeadersWith(token);

        ResponseEntity<String> created = rest.exchange(
                url("/group"), HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(
                        Map.of("name", "Ops", "description", "Operations")), h),
                String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(mapper.readTree(created.getBody()).get("name").asText())
                .isEqualTo("Ops");

        ResponseEntity<String> all = rest.exchange(
                url("/group"), HttpMethod.GET, new HttpEntity<>(h), String.class);
        JsonNode arr = mapper.readTree(all.getBody());
        assertThat(arr).hasSize(1);
        assertThat(arr.get(0).get("memberCount").asInt()).isZero();
    }

    /* ====================== Phase 2: microservice / endpoint / route ====================== */

    @Test
    void microserviceCreateAndListRoundTrip() throws Exception {
        String token = tokenFor("root", "ADMIN");
        String body = mapper.writeValueAsString(Map.of(
                "serviceId", "users-svc",
                "description", "Users microservice",
                "requestUri", "/users/**",
                "targetUriPath", "/api/v1",
                "targetUrlHost", "users.internal",
                "targetUrlPort", "8080"
        ));

        ResponseEntity<String> created = rest.exchange(
                url("/microservice/save"), HttpMethod.POST,
                new HttpEntity<>(body, jsonHeadersWith(token)), String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode createdJson = mapper.readTree(created.getBody());
        assertThat(createdJson.get("serviceId").asText()).isEqualTo("users-svc");

        ResponseEntity<String> all = rest.exchange(
                url("/microservice/getMicroservices"), HttpMethod.GET,
                new HttpEntity<>(jsonHeadersWith(token)), String.class);
        JsonNode arr = mapper.readTree(all.getBody());
        assertThat(arr).hasSize(1);
        assertThat(arr.get(0).get("serviceId").asText()).isEqualTo("users-svc");
        assertThat(arr.get(0).has("password")).isFalse();
    }

    @Test
    void microserviceDuplicateServiceIdReturns409() throws Exception {
        String token = tokenFor("root", "ADMIN");
        String body = mapper.writeValueAsString(Map.of("serviceId", "dup"));
        HttpHeaders h = jsonHeadersWith(token);

        rest.exchange(url("/microservice/save"), HttpMethod.POST,
                new HttpEntity<>(body, h), String.class);

        ResponseEntity<String> second = rest.exchange(
                url("/microservice/save"), HttpMethod.POST,
                new HttpEntity<>(body, h), String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(mapper.readTree(second.getBody()).get("code").asText())
                .isEqualTo("DUPLICATE");
    }

    @Test
    void endpointCreateBindMicroserviceAndCheckEndpoint() throws Exception {
        String token = tokenFor("root", "ADMIN");

        // Create a microservice to bind to.
        ResponseEntity<String> ms = rest.exchange(
                url("/microservice/save"), HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(Map.of("serviceId", "users-svc")),
                        jsonHeadersWith(token)), String.class);
        long microserviceId = mapper.readTree(ms.getBody()).get("id").asLong();

        // Create the endpoint.
        ResponseEntity<String> ep = rest.exchange(
                url("/endpoint/save"), HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(Map.of(
                                "method", "GET",
                                "path", "/api/users",
                                "description", "List users",
                                "numberParams", 0)),
                        jsonHeadersWith(token)), String.class);
        assertThat(ep.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long endpointId = mapper.readTree(ep.getBody()).get("id").asLong();

        // Bind the microservice.
        ResponseEntity<Void> bind = rest.exchange(
                url("/endpoint/" + endpointId + "/microservice/" + microserviceId),
                HttpMethod.POST, new HttpEntity<>(jsonHeadersWith(token)), Void.class);
        assertThat(bind.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verified by the checked listing.
        ResponseEntity<String> checked = rest.exchange(
                url("/endpoint/" + endpointId + "/microservices/checked"),
                HttpMethod.GET, new HttpEntity<>(jsonHeadersWith(token)), String.class);
        JsonNode rows = mapper.readTree(checked.getBody());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("serviceId").asText()).isEqualTo("users-svc");
        assertThat(rows.get(0).get("checked").asBoolean()).isTrue();
    }

    @Test
    void endpointDuplicatePathMethodDescriptionReturns409() throws Exception {
        String token = tokenFor("root", "ADMIN");
        String body = mapper.writeValueAsString(Map.of(
                "method", "GET", "path", "/dup", "description", "D", "numberParams", 0));
        HttpHeaders h = jsonHeadersWith(token);

        rest.exchange(url("/endpoint/save"), HttpMethod.POST,
                new HttpEntity<>(body, h), String.class);

        ResponseEntity<String> second = rest.exchange(
                url("/endpoint/save"), HttpMethod.POST,
                new HttpEntity<>(body, h), String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void endpointBindRoleAndCheckedListing() throws Exception {
        String token = tokenFor("root", "ADMIN");

        ResponseEntity<String> ep = rest.exchange(
                url("/endpoint/save"), HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(Map.of(
                                "method", "POST", "path", "/x", "description", "x", "numberParams", 0)),
                        jsonHeadersWith(token)), String.class);
        long endpointId = mapper.readTree(ep.getBody()).get("id").asLong();

        long adminRoleId = roleRepository.findByName("ADMIN").orElseThrow().getId();

        ResponseEntity<Void> bind = rest.exchange(
                url("/endpoint/" + endpointId + "/role/" + adminRoleId),
                HttpMethod.POST, new HttpEntity<>(jsonHeadersWith(token)), Void.class);
        assertThat(bind.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> checked = rest.exchange(
                url("/endpoint/" + endpointId + "/roles/checked"),
                HttpMethod.GET, new HttpEntity<>(jsonHeadersWith(token)), String.class);
        JsonNode rows = mapper.readTree(checked.getBody());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("name").asText()).isEqualTo("ADMIN");
        assertThat(rows.get(0).get("checked").asBoolean()).isTrue();
    }

    @Test
    void routeCreateWithLegacyZeroParentNormalizesToNull() throws Exception {
        String token = tokenFor("root", "ADMIN");
        String body = mapper.writeValueAsString(Map.of(
                "name", "Home",
                "path", "/home",
                "menuOrder", 1,
                "type", "MENU",
                "idParent", 0    // legacy "root" sentinel
        ));

        ResponseEntity<String> created = rest.exchange(
                url("/route/save"), HttpMethod.POST,
                new HttpEntity<>(body, jsonHeadersWith(token)), String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // The response and the persisted row should both have idParent = null.
        JsonNode json = mapper.readTree(created.getBody());
        assertThat(json.get("idParent").isNull()).isTrue();

        // /route/getRoutesByParent without an idParent should return roots only.
        ResponseEntity<String> roots = rest.exchange(
                url("/route/getRoutesByParent"), HttpMethod.GET,
                new HttpEntity<>(jsonHeadersWith(token)), String.class);
        JsonNode arr = mapper.readTree(roots.getBody());
        assertThat(arr).hasSize(1);
        assertThat(arr.get(0).get("name").asText()).isEqualTo("Home");
    }

    @Test
    void routeBindRoleAndCheckedListing() throws Exception {
        String token = tokenFor("root", "ADMIN");

        ResponseEntity<String> created = rest.exchange(
                url("/route/save"), HttpMethod.POST,
                new HttpEntity<>(mapper.writeValueAsString(Map.of(
                                "name", "Settings", "path", "/settings", "menuOrder", 2)),
                        jsonHeadersWith(token)), String.class);
        long routeId = mapper.readTree(created.getBody()).get("id").asLong();

        long adminRoleId = roleRepository.findByName("ADMIN").orElseThrow().getId();
        ResponseEntity<Void> bind = rest.exchange(
                url("/route/" + routeId + "/role/" + adminRoleId),
                HttpMethod.POST, new HttpEntity<>(jsonHeadersWith(token)), Void.class);
        assertThat(bind.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> checked = rest.exchange(
                url("/route/" + routeId + "/roles/checked"),
                HttpMethod.GET, new HttpEntity<>(jsonHeadersWith(token)), String.class);
        JsonNode rows = mapper.readTree(checked.getBody());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("name").asText()).isEqualTo("ADMIN");
        assertThat(rows.get(0).get("checked").asBoolean()).isTrue();
    }

    /* ====================== helpers ====================== */

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private String tokenFor(String username, String... roles) {
        Set<String> roleSet = new LinkedHashSet<>(List.of(roles));
        return jwtService.issueAccessToken(username, roleSet);
    }

    private HttpHeaders jsonHeadersWith(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }
}
