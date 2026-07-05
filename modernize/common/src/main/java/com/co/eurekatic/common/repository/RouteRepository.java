package com.co.eurekatic.common.repository;

import com.co.eurekatic.common.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Route}.
 *
 * <p>The legacy supports a tree view via
 * {@code getRoutesByParent(parentId)} with {@code 0} meaning
 * "root"; in the JPA port we keep two derived methods and let
 * the service layer decide which one to call.
 */
@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    /** All root routes (parent id is null in the modernized schema). */
    List<Route> findByIdParentIsNull();

    /** Direct children of a given parent route. */
    List<Route> findByIdParent(Long idParent);

    boolean existsByNameAndPath(String name, String path);

    /**
     * Used by the service layer to check uniqueness on update
     * (the legacy uniqueness rule on routes is name + path).
     */
    java.util.Optional<Route> findByNameAndPath(String name, String path);

    /**
     * Routes whose {@code id_app} FK points at the given app.
     * Powers the "primary" view of an app's routes and is one
     * of the two paths the {@code /myMenu} endpoint unions
     * when filtering by JWT roles.
     */
    List<Route> findAllByApp_Id(Long appId);

    /**
     * Routes whose {@code id_app} FK is null — "orphan"
     * routes that exist outside any app. The menu endpoint
     * treats these as "legacy / not yet categorized" and
     * surfaces them only when the user has a fine-grained
     * {@code role_route} binding for them.
     */
    List<Route> findAllByApp_IdIsNull();

    /**
     * Routes visible to a user whose JWT carries any of the
     * given role ids. The {@code /myMenu} endpoint uses this
     * to compute the per-user sidebar in one round-trip.
     *
     * <p>The query unions two paths:
     * <ol>
     *   <li><b>Broad</b> — the route's {@code id_app} is in
     *       {@code role_app} for one of the user's roles
     *       (the user sees all routes in that app).</li>
     *   <li><b>Fine-grained</b> — the route has a direct
     *       {@code ROLE_ROUTE} binding for one of the
     *       user's roles. This path catches orphan routes
     *       (where {@code id_app} is null) that should still
     *       be surfaced via per-route permission, plus
     *       routes that intentionally escape the app
     *       boundary.</li>
     * </ol>
     *
     * <p>Spring Data binds {@code :roleIds} as a Postgres
     * {@code BIGINT[]} via the JPQL {@code IN} clause. The
     * {@code DISTINCT} deduplicates routes that match both
     * paths (a route in an app that ALSO has a direct role
     * binding would otherwise appear twice).
     *
     * <p>Ordering: {@code MENUORDER} ASC breaks ties on
     * {@code ID_ROUTE} ASC — gives a stable order across
     * requests when two routes share the same
     * {@code menuOrder}.
     *
     * <p>Eager fetch of {@code app} — the response shape
     * includes {@code appId}/{@code appName} and we don't
     * want N+1 on a list endpoint.
     */
    @Query("""
            SELECT DISTINCT r FROM Route r
            LEFT JOIN FETCH r.app
            WHERE r.id IN (
                SELECT r2.id FROM Route r2
                 WHERE r2.app IS NOT NULL
                   AND r2.app.id IN (
                       SELECT ra.id FROM App ra JOIN ra.roles rol
                        WHERE rol.id IN :roleIds
                   )
                UNION
                SELECT r3.id FROM Route r3 JOIN r3.roles rol3
                 WHERE rol3.id IN :roleIds
            )
            ORDER BY r.menuOrder ASC, r.id ASC
            """)
    List<Route> findVisibleForRoles(@Param("roleIds") Collection<Long> roleIds);
}
