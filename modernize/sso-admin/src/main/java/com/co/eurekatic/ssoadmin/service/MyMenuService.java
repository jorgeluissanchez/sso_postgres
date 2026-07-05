package com.co.eurekatic.ssoadmin.service;

import com.co.eurekatic.common.repository.RoleRepository;
import com.co.eurekatic.common.repository.RouteRepository;
import com.co.eurekatic.ssoadmin.dto.RouteResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves the per-user sidebar menu from the JWT authorities.
 *
 * <p>The endpoint {@code GET /sso-admin/myMenu} consumes this
 * service; the frontend then renders the sidebar from the
 * returned routes. Today the sidebar in {@code admin-ui} is
 * hardcoded — once the FE is wired to this endpoint, the
 * hardcoded list disappears entirely.
 *
 * <p>Two flavors of authorization are honored (mirrors the
 * {@code /myQueries} pattern: broad + fine-grained both win):
 * <ul>
 *   <li><b>Broad</b> — the JWT carries a role bound to an
 *       {@link com.co.eurekatic.common.entity.App App} via
 *       {@code role_app}; the user sees all routes in that app.</li>
 *   <li><b>Fine-grained</b> — the JWT carries a role bound
 *       directly to the route via {@code role_route}; the user
 *       sees that specific route even if it's outside any app
 *       ("orphan" route).</li>
 * </ul>
 *
 * <p>JWT authorities come back as {@code ROLE_<NAME>} (see
 * {@link com.co.eurekatic.ssoadmin.config.JwtAuthenticationFilter}).
 * We strip the prefix to look up the {@link com.co.eurekatic.common.entity.Role Role}
 * entity by {@code name}, which is the authority stored in
 * {@code auth-center}'s JWT claims.
 */
@Service
public class MyMenuService {

    private final RouteRepository routeRepo;
    private final RoleRepository roleRepo;

    public MyMenuService(RouteRepository routeRepo,
                         RoleRepository roleRepo) {
        this.routeRepo = routeRepo;
        this.roleRepo = roleRepo;
    }

    /**
     * Returns the routes visible to the caller. Empty list is
     * a legitimate answer (the user has roles, but none of
     * them grant any menu access — admin should investigate
     * missing {@code role_app} bindings).
     */
    @Transactional(readOnly = true)
    public List<RouteResponse> forCaller(Authentication auth) {
        Set<Long> roleIds = roleIdsFromAuth(auth);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return routeRepo.findVisibleForRoles(roleIds).stream()
                .map(RouteResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Maps {@code ROLE_<NAME>} authorities to role ids by
     * name. The lookup is O(roles × roles_table); admin-ui
     * users typically have 2–5 roles, so the constant is
     * tiny and the in-memory {@code findAll} is fine. If
     * role counts ever explode, swap this for a batch
     * {@code findByNameIn(...)} on {@link RoleRepository}.
     */
    private Set<Long> roleIdsFromAuth(Authentication auth) {
        if (auth == null) return Set.of();
        Set<String> names = new LinkedHashSet<>();
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String a = ga.getAuthority();
            if (a != null && a.startsWith("ROLE_")) {
                names.add(a.substring("ROLE_".length()));
            }
        }
        if (names.isEmpty()) return Set.of();
        Set<Long> ids = new LinkedHashSet<>();
        roleRepo.findAll().forEach(r -> {
            if (names.contains(r.getName())) {
                ids.add(r.getId());
            }
        });
        return ids;
    }
}