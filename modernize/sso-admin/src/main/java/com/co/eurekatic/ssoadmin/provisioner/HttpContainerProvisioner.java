package com.co.eurekatic.ssoadmin.provisioner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Production {@link ContainerProvisioner} — translates the
 * spec into a {@code POST /provision} call to the
 * provisioner sidecar. The sidecar does the Docker Engine
 * API heavy lifting; sso-admin never opens the unix socket
 * directly.
 *
 * <p>Wire contract (matches the sidecar's REST surface —
 * see {@code provisioner/.../DockerContainerProvisioner}):
 * <pre>
 *   POST /provision         body: ProvisionSpec         → 201 { containerId }
 *   DELETE /provision/{name}                         → 204
 *   GET  /provision/{name}/status                   → 200 { status }
 *   GET  /actuator/health                          → 200 UP
 * </pre>
 *
 * <p>Error mapping:
 * <ul>
 *   <li>{@code 4xx} sidecar response (other than 404 on
 *       delete) → {@link ProvisioningException.Code#CONTAINER_CREATE_FAILED}
 *       with the sidecar's body in the message.</li>
 *   <li>{@code 5xx} or connection refused →
 *       {@link ProvisioningException.Code#SIDECAR_UNREACHABLE}.</li>
 *   <li>{@code DELETE} returning 404 is treated as success
 *       (idempotent).</li>
 * </ul>
 */
@Component
public class HttpContainerProvisioner implements ContainerProvisioner {

    private static final Logger log = LoggerFactory.getLogger(HttpContainerProvisioner.class);

    private final RestClient client;

    public HttpContainerProvisioner(ProvisionerClientConfig cfg) {
        this.client = RestClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public void provision(ProvisionSpec spec) {
        try {
            client.post()
                    .uri("/provision")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(spec)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new ProvisioningException(
                                ProvisioningException.Code.CONTAINER_CREATE_FAILED,
                                "Provisioner refused create for "
                                        + spec.instanceName() + ": " + res.getStatusText());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new ProvisioningException(
                                ProvisioningException.Code.CONTAINER_CREATE_FAILED,
                                "Provisioner 5xx on create for "
                                        + spec.instanceName() + ": " + res.getStatusText());
                    })
                    .toBodilessEntity();
            log.info("Provisioner accepted create for {}", spec.instanceName());
        } catch (ProvisioningException pe) {
            throw pe;
        } catch (RestClientException e) {
            log.warn("Provisioner unreachable on create for {}: {}",
                    spec.instanceName(), e.getMessage());
            throw new ProvisioningException(
                    ProvisioningException.Code.SIDECAR_UNREACHABLE,
                    "Provisioner unreachable: " + e.getMessage(), e);
        }
    }

    @Override
    public void deprovision(String fullInstanceName) {
        try {
            client.delete()
                    .uri("/provision/{name}", fullInstanceName)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        // 404 is the "already gone" success path
                        // for an idempotent delete — anything
                        // else is a real error.
                        if (res.getStatusCode().value() == 404) {
                            log.info("Provisioner reports {} already gone", fullInstanceName);
                            return;
                        }
                        throw new ProvisioningException(
                                ProvisioningException.Code.CONTAINER_CREATE_FAILED,
                                "Provisioner refused delete for "
                                        + fullInstanceName + ": " + res.getStatusText());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new ProvisioningException(
                                ProvisioningException.Code.CONTAINER_CREATE_FAILED,
                                "Provisioner 5xx on delete for "
                                        + fullInstanceName + ": " + res.getStatusText());
                    })
                    .toBodilessEntity();
        } catch (ProvisioningException pe) {
            throw pe;
        } catch (RestClientException e) {
            log.warn("Provisioner unreachable on delete for {}: {}",
                    fullInstanceName, e.getMessage());
            throw new ProvisioningException(
                    ProvisioningException.Code.SIDECAR_UNREACHABLE,
                    "Provisioner unreachable: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            client.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .onStatus(s -> true, (req, res) -> { /* swallow */ })
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            return false;
        }
    }
}