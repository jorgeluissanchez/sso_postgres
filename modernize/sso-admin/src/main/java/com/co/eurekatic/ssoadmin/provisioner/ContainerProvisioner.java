package com.co.eurekatic.ssoadmin.provisioner;

/**
 * Boundary the {@link com.co.eurekatic.ssoadmin.service.MicroserviceService}
 * uses to talk to the provisioner sidecar. The production
 * implementation is {@code HttpContainerProvisioner}, which uses
 * Spring's {@code RestClient} under the hood.
 *
 * <p>This interface exists for two reasons:
 * <ol>
 *   <li>The {@code MicroserviceService} unit tests can swap in
 *       a Mockito mock instead of booting an HTTP client.</li>
 *   <li>If we ever swap Docker Engine API for a Kubernetes
 *       operator, only this interface changes — the service
 *       stays put.</li>
 * </ol>
 */
public interface ContainerProvisioner {

    /**
     * Ask the provisioner to start a fresh {@code query-service}
     * container matching the spec. Blocks until the
     * {@code POST /v1.43/containers/create} + {@code /start}
     * round-trip completes.
     *
     * @throws ProvisioningException when the sidecar is
     *         unreachable, the Docker daemon rejects the
     *         create, or the container fails to stay up.
     */
    void provision(ProvisionSpec spec);

    /**
     * Best-effort delete of the container with the given
     * full instance id ({@code query-service-<suffix>}). Never
     * throws on a missing container — that is the success
     * path for an idempotent delete.
     *
     * @throws ProvisioningException only when the sidecar is
     *         unreachable.
     */
    void deprovision(String fullInstanceName);

    /**
     * Cheap check that the sidecar is alive (used by the
     * {@code /actuator/health} downstream chain, not by
     * {@code MicroserviceService}).
     */
    boolean isHealthy();
}