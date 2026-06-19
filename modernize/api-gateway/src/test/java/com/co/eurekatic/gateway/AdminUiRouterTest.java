package com.co.eurekatic.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AdminUiRouter}.
 *
 * In a test environment without a built SPA, the resources
 * `static/admin/index.html` and `static/admin/assets/...` don't
 * exist. The router therefore returns 404 for those paths. That's
 * fine — the SPA is served only in production images built by
 * api-gateway/Dockerfile (stage `node-spa` -> `static/admin/`).
 *
 * What we verify here is that:
 *  1. The bean is registered and wired into the WebFlux chain.
 *  2. `/admin/**` paths are NOT 401 — they reach the router and
 *     don't get short-circuited by the security filter. The router
 *     itself can return 404, but it must not return 401.
 *  3. `/sso-admin/**` paths (the API gateway route) still flow
 *     through Spring Cloud Gateway, not the static router.
 *  4. Security for non-SPA paths is unchanged: `/protected` is 401.
 *
 * <p>End-to-end verification (curl returning the SPA index.html for
 * `/admin/users`, the JS chunk for `/admin/assets/index-abc.js`)
 * requires a built SPA, which only happens inside the Docker image.
 * Run {@code docker compose up -d --build api-gateway} and curl
 * {@code http://localhost:8080/admin/} to verify.
 */
@SpringBootTest(
        classes = AdminUiRouterTest.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = {
        "sso.jwt.secret=gateway-integration-test-secret-32-bytes-or-more-please-thank-you-1234",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class AdminUiRouterTest {

    @LocalServerPort
    int port;

    @Autowired
    AdminUiRouter adminUiRouter;

    @Test
    void adminUiRouterBeanIsRegistered() {
        assertThat(adminUiRouter).isNotNull();
    }

    @Test
    void adminPathIsNotRejectedBySecurity() {
        // The SPA fallback would 404 in this test (no built dist),
        // but critically it is NOT 401. That means SecurityWebFilterChain
        // permits /admin/** and the RouterFunction gets a chance to run.
        webTestClient().get().uri("/admin/users").exchange()
                .expectStatus().isNotEqualTo(401);
    }

    @Test
    void adminRootIsNotRejectedBySecurity() {
        webTestClient().get().uri("/admin/").exchange()
                .expectStatus().isNotEqualTo(401);
    }

    @Test
    void adminAssetPathIsNotRejectedBySecurity() {
        // /admin/assets/whatever.js — without a built SPA, the
        // resource doesn't exist; the router returns 404. The point
        // is the request reaches the router, not the security filter.
        webTestClient().get().uri("/admin/assets/index-abc.js").exchange()
                .expectStatus().isNotEqualTo(401);
    }

    @Test
    void nonAdminProtectedPathIsStill401() {
        // The router only matches /admin/**. Other protected paths
        // still get 401 from Spring Security.
        webTestClient().get().uri("/protected").exchange()
                .expectStatus().isEqualTo(401);
    }

    @Test
    void ssoAdminApiPathsStillReachGatewayRouting() {
        // /sso-admin/** is owned by a SCG route, not the SPA router.
        // Without a real sso-admin upstream, we expect a 5xx or a
        // gateway timeout — anything except a 404 from the SPA
        // router (which doesn't claim this prefix).
        int status = webTestClient().get().uri("/sso-admin/getUsers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
                .exchange()
                .returnResult(String.class)
                .getStatus()
                .value();
        // We don't assert a specific code because the SCG behaviour
        // with no registered downstream varies by Spring Cloud
        // version, but we DO assert it's not 404 from the SPA router.
        // The SPA router returns 404 only for /admin/**.
        assertThat(status).isNotEqualTo(404);
    }

    /* ====================== helpers ====================== */

    private WebTestClient webTestClient() {
        return WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @TestConfiguration
    static class TestApp {
        @Bean
        public ApiGatewayApplication main() {
            return new ApiGatewayApplication();
        }

        @Bean
        @Primary
        public ReactiveDiscoveryClient stubDiscoveryClient() {
            return new ReactiveDiscoveryClient() {
                @Override
                public Flux<String> getServices() {
                    return Flux.empty();
                }

                @Override
                public Flux<ServiceInstance> getInstances(String serviceId) {
                    return Flux.empty();
                }

                @Override
                public String description() {
                    return "stub-discovery-client";
                }
            };
        }
    }
}
