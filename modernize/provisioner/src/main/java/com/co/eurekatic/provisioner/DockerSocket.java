package com.co.eurekatic.provisioner;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Talks to the Docker Engine API over the unix socket
 * mounted at {@code /var/run/docker.sock} by spawning
 * {@code curl --unix-socket}. Why curl and not the JDK's
 * {@link java.net.http.HttpClient}: the JDK's unix-socket
 * support requires either an incubating preview API or a
 * custom {@code SocketImpl} hack. {@code curl} is in every
 * base image we ship, doesn't add a dependency, and the
 * call shape (POST/DELETE/GET with a JSON body) maps 1:1 to
 * the Docker API surface.
 *
 * <p>Test seam: this class is the only {@code curl}-spawning
 * code in the app, so a unit test can swap in a fake
 * {@link DockerSocket} via the {@code DockerCommand} wrapper
 * if we ever need to. In practice the integration tests use
 * Testcontainers against a real Docker daemon, which is
 * what the existing modules do.
 */
@Component
public class DockerSocket {

    private static final Logger log = LoggerFactory.getLogger(DockerSocket.class);

    private final ProvisionerProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    public DockerSocket(ProvisionerProperties props) {
        this.props = props;
    }

    /**
     * {@code POST /v1.43/containers/create} + {@code POST
     * /v1.43/containers/{id}/start}. Returns the Docker
     * container id.
     *
     * @throws DockerException on any non-zero curl exit or
     *         Docker-reported error.
     */
    public String createAndStart(ProvisionRequest req, String fullInstanceName) {
        // POST /containers/create — body is a JSON spec
        // listing image, env, network on create (we attach
        // the container to the same compose network that
        // sso-admin lives on so service-name DNS works).
        String createBody = mapper.writeValueAsString(Map.of(
                "Image", props.getImage(),
                "Env", buildEnv(req),
                "ExposedPorts", Map.of("8084/tcp", Map.of()),
                "HostConfig", Map.of(
                        "NetworkMode", props.getNetwork(),
                        "RestartPolicy", Map.of("Name", "unless-stopped"),
                        "PortBindings", Map.of(
                                "8084/tcp", List.of(Map.of("HostPort", "8084"))))));

        JsonNode createResp = exec(List.of(
                "curl", "-sS", "-X", "POST",
                "--unix-socket", props.getSocketPath(),
                "-H", "Content-Type: application/json",
                "-d", createBody,
                "http://localhost/v1.43/containers/create?name=" + fullInstanceName));

        String containerId = createResp.path("Id").asText(null);
        if (containerId == null || containerId.isBlank()) {
            throw new DockerException("containers/create returned no Id: " + createResp);
        }

        exec(List.of(
                "curl", "-sS", "-X", "POST",
                "--unix-socket", props.getSocketPath(),
                "http://localhost/v1.43/containers/" + containerId + "/start"));

        log.info("Started container {} for {}", containerId, fullInstanceName);
        return containerId;
    }

    /**
     * {@code POST /v1.43/containers/{name}/stop} + {@code DELETE
     * /v1.43/containers/{name}?force=true}. Treats 404 as
     * success (idempotent delete).
     */
    public void stopAndRemove(String fullInstanceName) {
        // stop — best effort, ignore failures (already stopped)
        try {
            exec(List.of(
                    "curl", "-sS", "-X", "POST",
                    "--unix-socket", props.getSocketPath(),
                    "http://localhost/v1.43/containers/" + fullInstanceName + "/stop"));
        } catch (DockerException e) {
            log.debug("stop ignored for {}: {}", fullInstanceName, e.getMessage());
        }
        // remove with force=true — kills running container
        // if stop didn't take.
        try {
            exec(List.of(
                    "curl", "-sS", "-X", "DELETE",
                    "--unix-socket", props.getSocketPath(),
                    "http://localhost/v1.43/containers/" + fullInstanceName + "?force=true"));
        } catch (DockerException e) {
            if (e.getMessage().contains("404")) {
                log.info("Container {} already gone", fullInstanceName);
                return;
            }
            throw e;
        }
    }

    /**
     * {@code GET /v1.43/containers/{name}/json}. Returns
     * the Docker state string ({@code running}, {@code exited},
     * etc.) or null if the container doesn't exist.
     */
    public String status(String fullInstanceName) {
        try {
            JsonNode resp = exec(List.of(
                    "curl", "-sS",
                    "--unix-socket", props.getSocketPath(),
                    "http://localhost/v1.43/containers/" + fullInstanceName + "/json"));
            return resp.path("State").path("Status").asText(null);
        } catch (DockerException e) {
            if (e.getMessage().contains("404")) {
                return null;
            }
            throw e;
        }
    }

    private List<String> buildEnv(ProvisionRequest req) {
        // The instance id (e.g. query-service-oracle-dev) is
        // what query-service registers with Eureka under. We
        // set spring.application.name via INSTANCE_NAME so
        // the InstanceNameResolver picks the right service-id
        // and the gateway's discovery.locator creates a
        // matching route.
        List<String> env = new ArrayList<>();
        env.add("QUERY_DS_DIALECT=" + req.dialect());
        env.add("QUERY_DS_URL=" + req.jdbcUrl());
        env.add("QUERY_DS_USERNAME=" + req.dbUsername());
        if (req.dbPassword() != null) {
            env.add("QUERY_DS_PASSWORD=" + req.dbPassword());
        }
        env.add("QUERY_DS_POOL_SIZE=" + req.poolSize());
        env.add("QUERY_INSTANCE_NAME=" + req.instanceName());
        env.add("EUREKA_URL=" + props.getEurekaUrl());
        // JWT_SECRET must match auth-center's — the
        // query-service instance validates Bearer tokens
        // with the same HMAC key. Operator sets it in the
        // provisioner's compose env block.
        if (props.getJwtSecret() != null && !props.getJwtSecret().isBlank()) {
            env.add("JWT_SECRET=" + props.getJwtSecret());
        }
        // The catalog endpoint URL the new query-service
        // instance uses to resolve uuid→SQL. Defaults to
        // the gateway address, but operators can override
        // via environment if their network topology is
        // different.
        env.add("QUERY_CATALOG_BASE_URL=http://sso-api-gateway:8080/sso-admin");
        return env;
    }

    private JsonNode exec(List<String> command) {
        try {
            Process p = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            String output;
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                output = sb.toString();
            }
            if (!p.waitFor(30, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new DockerException("curl timed out: " + command);
            }
            int exit = p.exitValue();
            if (exit != 0) {
                throw new DockerException("curl exited " + exit + ": " + output);
            }
            if (output.isBlank()) {
                return mapper.createObjectNode();
            }
            return mapper.readTree(output);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new DockerException("curl spawn failed: " + e.getMessage(), e);
        }
    }

    /** Runtime exception wrapping any Docker Engine API failure. */
    public static class DockerException extends RuntimeException {
        public DockerException(String message) { super(message); }
        public DockerException(String message, Throwable cause) { super(message, cause); }
    }
}