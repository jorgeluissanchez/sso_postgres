package com.co.eurekatic.common.repository;

import com.co.eurekatic.common.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
