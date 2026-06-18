package com.co.eurekatic.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Microservice — a single downstream HTTP service that the
 * gateway can route to. Mirrors the legacy
 * {@code MICROSERVICE} table from {@code sso-service}.
 *
 * <p>The table name keeps the legacy {@code UPPER_CASE} style
 * (no pluralization, no snake_case) so cross-table joins
 * (especially the ones the legacy SQL uses in raw queries) keep
 * working with the same identifiers. This is a deliberate
 * compromise: the modernized entities use camelCase fields and
 * proper JPA conventions, but the table layout matches the
 * legacy until we explicitly decide to migrate it.
 *
 * <p>Relations:
 * <ul>
 *   <li>{@link #endpoints} — many-to-many. A microservice
 *       exposes 0..N endpoints. The owning side of the
 *       many-to-many is on {@code Endpoint.microservices} via
 *       the {@code ENDPOINT_MICROSERVICE} join table. Call
 *       {@code endpoint.addMicroservice(ms)} to attach.</li>
 * </ul>
 *
 * <p>Notes:
 * <ul>
 *   <li>{@code CREATEDDATE} is read-only — the legacy never
 *       inserted it from Java, and the DB populates it. JPA
 *       marks it {@code insertable=false, updatable=false} so
 *       we don't fight the DB default.</li>
 *   <li>There is no direct Microservice↔Role join table; the
 *       legacy uses {@code ROLE_ENDPOINT} and infers microservice
 *       access from endpoint bindings. We follow the same model.</li>
 * </ul>
 */
@Entity
@Table(name = "MICROSERVICE")
@Getter
@Setter
@NoArgsConstructor
public class Microservice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_MICROSERVICE")
    private Long id;

    @Column(name = "SERVICEID", nullable = false, length = 200)
    private String serviceId;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "REQUESTURI", length = 500)
    private String requestUri;

    @Column(name = "TARGETURIPATH", length = 500)
    private String targetUriPath;

    @Column(name = "TARGETURLHOST", length = 500)
    private String targetUrlHost;

    @Column(name = "TARGETURLPORT", length = 10)
    private String targetUrlPort;

    /**
     * DB-managed timestamp. Never written from Java — the
     * schema defaults it to {@code now()} on insert.
     */
    @Column(name = "CREATEDDATE", insertable = false, updatable = false)
    private String createdDate;

    /**
     * Endpoints exposed by this microservice. The owning side
     * of the relation is on {@link Endpoint#getMicroservices()}
     * — to attach, call {@code endpoint.addMicroservice(this)},
     * not the setter on this side.
     */
    @ManyToMany(mappedBy = "microservices", fetch = FetchType.LAZY)
    @Setter(AccessLevel.NONE)
    private Set<Endpoint> endpoints = new HashSet<>();

    /* ====================== equality ====================== */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Microservice other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
