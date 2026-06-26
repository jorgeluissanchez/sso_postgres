package com.co.eurekatic.provisioner;

/**
 * Response body for {@code POST /provision} (201) and
 * {@code GET /provision/{name}/status} (200).
 */
public record ProvisionResponse(
        /** Full instance id, e.g. {@code query-service-oracle-dev}. */
        String instanceName,
        /** Container id returned by the Docker Engine API. */
        String containerId,
        /** Lifecycle state from Docker: created | running | exited | removing. */
        String status
) {}