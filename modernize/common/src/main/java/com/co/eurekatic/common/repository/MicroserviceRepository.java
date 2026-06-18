package com.co.eurekatic.common.repository;

import com.co.eurekatic.common.entity.Microservice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Microservice}.
 *
 * <p>Most queries are simple {@code findAll} / {@code findById}
 * — the controller layer needs a method to look up by
 * {@code serviceId} (the natural key from the legacy
 * {@code getMicroservice?serviceId=…} endpoint) and an
 * existence check used for duplicate-rejection in
 * {@code createMicroservice}.
 */
@Repository
public interface MicroserviceRepository extends JpaRepository<Microservice, Long> {

    Optional<Microservice> findByServiceId(String serviceId);

    boolean existsByServiceId(String serviceId);
}
