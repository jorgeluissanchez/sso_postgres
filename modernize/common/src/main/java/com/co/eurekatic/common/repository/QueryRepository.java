package com.co.eurekatic.common.repository;

import com.co.eurekatic.common.entity.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;

/**
 * Repository for {@link Query}. The catalog endpoint needs the
 * query AND its bound roles in a single round-trip so the
 * authorization join (uuid + username via roles) can run; we
 * eagerly fetch the roles collection on {@link #findByUuid(String)}
 * via an {@link EntityGraph}.
 */
@Repository
public interface QueryRepository extends JpaRepository<Query, Long> {

    /**
     * Look up by the public {@code uuid} handle. Eagerly loads
     * {@code roles} so the catalog endpoint can decide permission
     * without an N+1 follow-up query. The unique index on
     * {@code UUID} keeps this O(1).
     */
    @EntityGraph(attributePaths = "roles")
    @QueryHints(@jakarta.persistence.QueryHint(name = HINT_FETCH_SIZE, value = "1"))
    Optional<Query> findByUuid(String uuid);

    boolean existsByUuid(String uuid);
}
