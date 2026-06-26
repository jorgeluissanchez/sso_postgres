package com.co.eurekatic.ssoadmin.service;

import com.co.eurekatic.common.entity.Microservice;
import com.co.eurekatic.common.repository.MicroserviceRepository;
import com.co.eurekatic.ssoadmin.dto.MicroserviceRequest;
import com.co.eurekatic.ssoadmin.dto.MicroserviceResponse;
import com.co.eurekatic.ssoadmin.exception.DuplicateException;
import com.co.eurekatic.ssoadmin.exception.NotFoundException;
import com.co.eurekatic.ssoadmin.provisioner.ContainerProvisioner;
import com.co.eurekatic.ssoadmin.provisioner.EurekaReadinessProbe;
import com.co.eurekatic.ssoadmin.provisioner.ProvisionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Microservice CRUD. The legacy
 * {@code com.co.lowcode.sso.service.MicroserviceService} also
 * exposed an {@code endpoint/checked} listing and a binding
 * helper; those concerns live on {@link EndpointService} in
 * the modern port (the join table is owned by Endpoint).
 *
 * <p><b>Provisioning.</b> For rows with
 * {@link MicroserviceRequest#kind() kind=QUERY} this service
 * drives the dynamic query-service container lifecycle:
 * <ol>
 *   <li>{@link #create(MicroserviceRequest)} persists the row
 *       first, then calls {@link ContainerProvisioner#provision(ProvisionSpec)}
 *       (which translates the spec into a
 *       {@code POST /v1.43/containers/create} against the Docker
 *       daemon via the provisioner sidecar).</li>
 *   <li>Once the container is up, {@link EurekaReadinessProbe#waitForInstance(String)}
 *       polls the local {@code DiscoveryClient} until the new
 *       instance appears under the expected service-id
 *       ({@code query-service-<instanceName>} or
 *       {@code query-service-<dialect>}). Only then does
 *       {@code create} return — the gateway can route traffic
 *       to the new container by the time the call completes.</li>
 *   <li>{@link #delete(Long)} reverses the flow best-effort:
 *       the row is dropped even if the provisioner is
 *       unreachable (logged as WARN), so admin-ui never gets
 *       stuck retrying a delete that the user has already
 *       confirmed.</li>
 * </ol>
 *
 * <p>REST rows take no provisioner path — the
 * {@code ContainerProvisioner} bean is only invoked when
 * {@code "QUERY".equals(req.kind())}.
 */
@Service
public class MicroserviceService {

    private static final Logger log = LoggerFactory.getLogger(MicroserviceService.class);

    private final MicroserviceRepository repo;
    private final ContainerProvisioner provisioner;
    private final EurekaReadinessProbe readinessProbe;

    public MicroserviceService(MicroserviceRepository repo,
                               ContainerProvisioner provisioner,
                               EurekaReadinessProbe readinessProbe) {
        this.repo = repo;
        this.provisioner = provisioner;
        this.readinessProbe = readinessProbe;
    }

    @Transactional
    public MicroserviceResponse create(MicroserviceRequest req) {
        if (repo.existsByServiceId(req.serviceId())) {
            throw new DuplicateException("Microservice", req.serviceId());
        }

        // Default the kind discriminator if the client omitted it
        // (REST is the legacy behavior).
        String kind = (req.kind() == null || req.kind().isBlank()) ? "REST" : req.kind();
        if (!"REST".equals(kind) && !"QUERY".equals(kind)) {
            throw new IllegalArgumentException("kind must be REST or QUERY, got: " + kind);
        }

        // Cross-field validation for QUERY rows. Bean Validation
        // doesn't compose well across fields; doing it here keeps
        // the DTO annotation list flat.
        if ("QUERY".equals(kind)) {
            requireQueryField(req.dialect(), "dialect");
            requireQueryField(req.jdbcUrl(), "jdbcUrl");
            requireQueryField(req.dbUsername(), "dbUsername");
            if (req.instanceName() != null
                    && repo.existsByInstanceName(req.instanceName())) {
                throw new DuplicateException("Microservice.instanceName",
                        req.instanceName());
            }
        }

        Microservice m = new Microservice();
        copy(req, m);
        m.setKind(kind);
        Microservice saved = repo.save(m);

        if ("QUERY".equals(kind)) {
            // Provision first (container create is fast-fail), then
            // wait for Eureka. If either fails the surrounding
            // transaction rolls back and no orphan row is left —
            // the ProvisioningException propagates to the
            // controller's error handler.
            String instanceId = req.instanceName() != null
                    ? req.instanceName()
                    : req.dialect();
            provisioner.provision(new ProvisionSpec(
                    instanceId,
                    req.dialect(),
                    req.jdbcUrl(),
                    req.dbUsername(),
                    req.dbPassword(),
                    req.poolSize() != null ? req.poolSize() : 10));
            readinessProbe.waitForInstance("query-service-" + instanceId);
            log.info("Provisioned query-service instance {} (dialect={})",
                    instanceId, req.dialect());
        }

        return MicroserviceResponse.fromEntity(saved);
    }

    @Transactional
    public MicroserviceResponse update(MicroserviceRequest req) {
        if (req.id() == null) {
            throw new IllegalArgumentException("id is required for update");
        }
        Microservice m = repo.findById(req.id())
                .orElseThrow(() -> new NotFoundException("Microservice", req.id()));
        copy(req, m);
        return MicroserviceResponse.fromEntity(repo.save(m));
    }

    @Transactional(readOnly = true)
    public List<MicroserviceResponse> getAll() {
        return repo.findAll().stream()
                .map(MicroserviceResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public MicroserviceResponse getById(Long id) {
        Microservice m = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Microservice", id));
        return MicroserviceResponse.fromEntity(m);
    }

    @Transactional(readOnly = true)
    public MicroserviceResponse getByServiceId(String serviceId) {
        Microservice m = repo.findByServiceId(serviceId)
                .orElseThrow(() -> new NotFoundException("Microservice", serviceId));
        return MicroserviceResponse.fromEntity(m);
    }

    @Transactional
    public void delete(Long id) {
        Microservice m = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Microservice", id));

        // Best-effort: drop the row even if deprovisioning
        // blows up — leaving an orphan row on disk is worse
        // than leaving an orphan container, because the user
        // can't recover from the row state without manual SQL.
        if ("QUERY".equals(m.getKind())) {
            String instanceId = m.getInstanceName() != null
                    ? m.getInstanceName()
                    : m.getDialect();
            try {
                provisioner.deprovision("query-service-" + instanceId);
                log.info("Deprovisioned query-service instance {}", instanceId);
            } catch (RuntimeException ex) {
                log.warn("Deprovision failed for instance {}; row will still be removed: {}",
                        instanceId, ex.getMessage());
            }
        }

        repo.delete(m);
    }

    /* ------------- internals ------------- */

    private static void copy(MicroserviceRequest req, Microservice m) {
        m.setServiceId(req.serviceId());
        m.setDescription(req.description());
        m.setRequestUri(req.requestUri());
        m.setTargetUriPath(req.targetUriPath());
        m.setTargetUrlHost(req.targetUrlHost());
        m.setTargetUrlPort(req.targetUrlPort());
        // QUERY-only fields. REST rows leave these null — the
        // DB defaults for KIND keep the discriminator sane even
        // when the client never sent one.
        m.setDialect(req.dialect());
        m.setJdbcUrl(req.jdbcUrl());
        m.setDbUsername(req.dbUsername());
        m.setDbPassword(req.dbPassword());
        m.setPoolSize(req.poolSize());
        m.setInstanceName(req.instanceName());
    }

    private static void requireQueryField(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    name + " is required when kind=QUERY");
        }
    }
}
