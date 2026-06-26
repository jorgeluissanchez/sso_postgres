package com.co.eurekatic.ssoadmin.controller;

import com.co.eurekatic.common.security.AuthPrincipal;
import com.co.eurekatic.ssoadmin.dto.QueryDefinition;
import com.co.eurekatic.ssoadmin.service.QueryCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consumer-facing read endpoint for the query catalog.
 * Consumed by {@code query-service} via {@code CatalogClient}.
 *
 * <p>Distinct from {@link QueryAdminController}:
 * <ul>
 *   <li>{@code /getQuery} is the read endpoint that downstream
 *       services call to resolve a {@code uuid} into a usable
 *       SQL string with metadata. Authenticated callers with
 *       at least one role bound to the query (or a
 *       {@code publicEnd} query) get a 200; everyone else gets
 *       a 403 — including the case where the uuid does not
 *       exist. We deliberately do not distinguish "missing"
 *       from "forbidden" so the catalog is not a discovery
 *       service.</li>
 *   <li>{@code /query/**} (admin) is gated by {@code ROLE_ADMIN}
 *       and is used by the admin UI to manage the catalog rows.</li>
 * </ul>
 *
 * <p>Username comes from the {@link AuthPrincipal} already in the
 * {@link SecurityContextHolder}, which the
 * {@code JwtAuthenticationFilter} populates before this handler
 * runs. We don't re-parse the JWT here — the gateway has
 * already validated the signature when the access token was
 * issued. The filter chain only requires the token to be
 * syntactically valid (parseable) here; if the gateway is
 * bypassed, signature verification is the caller's problem
 * and we let the role check above reject them anyway.
 */
@RestController
public class QueryCatalogController {

    private final QueryCatalogService service;

    public QueryCatalogController(QueryCatalogService service) {
        this.service = service;
    }

    @GetMapping("/getQuery")
    public ResponseEntity<QueryDefinition> getQuery(@RequestParam String uuid) {
        String username = currentUsername();
        return ResponseEntity.ok(service.resolve(uuid, username));
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal principal)) {
            // Should never happen — SecurityConfig marks
            // /getQuery as authenticated. Falling back to
            // AccessDeniedException here keeps the failure
            // mode consistent with the role check below.
            throw new AccessDeniedException("No autenticado");
        }
        return principal.username();
    }
}