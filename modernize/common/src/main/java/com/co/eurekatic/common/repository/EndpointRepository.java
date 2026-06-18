package com.co.eurekatic.common.repository;

import com.co.eurekatic.common.entity.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Endpoint}.
 *
 * <p>The legacy enforces uniqueness on
 * {@code (PATH, METHOD, DESCRIPTION)}. The modern port keeps
 * that contract via a {@code @UniqueConstraint} on the
 * {@code ENDPOINT} table (see the Phase 2 SQL init script)
 * and uses a derived {@code exists} query for the friendly
 * pre-flight duplicate check in the service layer.
 */
@Repository
public interface EndpointRepository extends JpaRepository<Endpoint, Long> {

    boolean existsByPathAndMethodAndDescription(String path, String method, String description);

    Optional<Endpoint> findByPathAndMethodAndDescription(String path, String method, String description);
}
