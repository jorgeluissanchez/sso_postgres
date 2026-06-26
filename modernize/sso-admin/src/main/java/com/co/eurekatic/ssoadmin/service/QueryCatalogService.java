package com.co.eurekatic.ssoadmin.service;

import com.co.eurekatic.common.entity.Query;
import com.co.eurekatic.common.entity.Role;
import com.co.eurekatic.common.repository.QueryRepository;
import com.co.eurekatic.common.repository.UserRepository;
import com.co.eurekatic.ssoadmin.dto.QueryDefinition;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read-side service backing {@code GET /getQuery?uuid=...}.
 *
 * <p>Authorization model: the caller must have at least one role
 * in {@link Query#getRoles()}. The check is enforced here (not
 * in Spring Security) because the policy is per-row, not
 * per-endpoint — the same URL works for many different queries
 * with different role bindings.
 *
 * <p>Why {@code EntityGraph} on the repository: the roles
 * collection is {@code LAZY}, but authorization needs to read
 * it. Without the eager fetch, the @Transactional boundary ends
 * before the lazy load and we get a
 * {@code LazyInitializationException}. The repository already
 * declares {@code findByUuid} with {@code @EntityGraph}; we just
 * consume it here.
 *
 * <p>Public-endpoint bypass: if {@link Query#isPublicEnd()} is
 * true, the username is not required to have a bound role —
 * any authenticated caller (or, in the gateway config, a
 * permitAll path) can resolve it. This matches the legacy
 * {@code PUBLIC_END} semantics: a query marked public is
 * addressable without further permission checks. Captcha is
 * still enforced downstream by {@code query-service} when the
 * {@code QueryDefinition#captcha()} flag is true.
 */
@Service
public class QueryCatalogService {

    private final QueryRepository queryRepo;
    private final UserRepository userRepo;

    public QueryCatalogService(QueryRepository queryRepo,
                               UserRepository userRepo) {
        this.queryRepo = queryRepo;
        this.userRepo = userRepo;
    }

    /**
     * Resolves the query definition for the given uuid, checking
     * that {@code username} has at least one role bound to the
     * query (or the query is {@code publicEnd}).
     *
     * @throws AccessDeniedException if the query is unknown OR
     *         the user has no role that grants access. We
     *         deliberately use the SAME exception for both cases
     *         so a probe for the existence of a uuid gets the
     *         same response as a denied access — the catalog is
     *         not a discovery service.
     */
    @Transactional(readOnly = true)
    public QueryDefinition resolve(String uuid, String username) {
        Query q = queryRepo.findByUuid(uuid)
                .orElseThrow(() -> new AccessDeniedException(
                        "No tiene acceso al query: " + uuid));

        if (q.isPublicEnd()) {
            return QueryDefinition.fromEntity(q);
        }

        // Role check: the user's roles must intersect the
        // query's bound roles. The username → roles lookup
        // goes through userRepo (which loads User.roles
        // EAGER — see User.java) so no lazy issue here.
        Set<String> userRoles = userRepo.findByUsername(username)
                .map(u -> u.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
        Set<String> queryRoles = q.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        boolean authorized = userRoles.stream().anyMatch(queryRoles::contains);
        if (!authorized) {
            throw new AccessDeniedException(
                    "No tiene acceso al query: " + uuid);
        }

        return QueryDefinition.fromEntity(q);
    }
}
