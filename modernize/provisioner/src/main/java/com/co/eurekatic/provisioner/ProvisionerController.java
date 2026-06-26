package com.co.eurekatic.provisioner;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST surface of the provisioner sidecar. The contract is
 * intentionally tiny — three endpoints cover the full
 * lifecycle that sso-admin drives:
 *
 * <pre>
 *   POST   /provision             body: ProvisionRequest   → 201 ProvisionResponse
 *   DELETE /provision/{name}                                → 204
 *   GET    /provision/{name}/status                         → 200 ProvisionResponse
 * </pre>
 *
 * <p>Error mapping: 4xx for caller mistakes (bad spec),
 * 5xx for Docker-side failures (image pull failed,
 * container start crashed). The {@code /actuator/health}
 * endpoint comes for free from the actuator starter and
 * is what sso-admin's {@code HttpContainerProvisioner#isHealthy}
 * polls.
 */
@RestController
@RequestMapping("/provision")
public class ProvisionerController {

    private final DockerSocket docker;

    public ProvisionerController(DockerSocket docker) {
        this.docker = docker;
    }

    @PostMapping
    public ResponseEntity<ProvisionResponse> provision(@Valid @RequestBody ProvisionRequest req) {
        String fullName = "query-service-" + req.instanceName();
        String containerId = docker.createAndStart(req, fullName);
        // Status at create+start time is always "running"
        // (start is synchronous); the readiness probe on
        // sso-admin's side is what waits for the JVM to be
        // healthy enough to register with Eureka.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ProvisionResponse(fullName, containerId, "running"));
    }

    @DeleteMapping("/{fullName}")
    public ResponseEntity<Void> deprovision(@PathVariable String fullName) {
        docker.stopAndRemove(fullName);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{fullName}/status")
    public ResponseEntity<ProvisionResponse> status(@PathVariable String fullName) {
        String state = docker.status(fullName);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new ProvisionResponse(fullName, fullName, state));
    }
}