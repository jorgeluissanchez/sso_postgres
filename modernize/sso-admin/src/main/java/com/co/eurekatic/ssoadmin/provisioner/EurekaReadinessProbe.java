package com.co.eurekatic.ssoadmin.provisioner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Polls the local {@link DiscoveryClient} until a fresh
 * query-service instance appears under the expected
 * service-id, or the timeout elapses.
 *
 * <p>Why this is necessary: even after the provisioner
 * returns 201 Created, the new container still has to
 * (a) finish its JVM warm-up, (b) register its Eureka
 * heartbeat, (c) get picked up by the api-gateway's
 * {@code discovery.locator}. Without this probe, the
 * controller would return before the gateway could route
 * to the new instance — first user request would 503.
 *
 * <p>The probe uses sso-admin's own
 * {@link DiscoveryClient} rather than hitting Eureka
 * directly. Spring Cloud Netflix caches the registry
 * locally with the {@code registry-fetch-interval-seconds}
 * value configured in {@code application.yml} (5s in dev);
 * that interval is the lower bound on how fast a new
 * instance becomes visible here. The probe's poll
 * interval (1s) is below that — we sleep and re-poll
 * until either the instance shows up or the overall
 * deadline fires.
 */
@Component
public class EurekaReadinessProbe {

    private static final Logger log = LoggerFactory.getLogger(EurekaReadinessProbe.class);

    /** Poll interval — must be ≤ Eureka fetch interval or we'll miss heartbeats. */
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

    /** Total wait before giving up. Generous because the
     *  JVM warm-up window is the dominant cost in dev
     *  (the sso query-service takes ~30s to be UP). */
    private static final Duration TIMEOUT = Duration.ofSeconds(45);

    private final DiscoveryClient discoveryClient;

    public EurekaReadinessProbe(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    /**
     * Blocks until the given service-id has at least one
     * known instance, or throws {@link ProvisioningException}
     * with {@link ProvisioningException.Code#EUREKA_TIMEOUT}.
     *
     * @param serviceId The Eureka service-id to wait for
     *                  (e.g. {@code query-service-oracle-dev}).
     */
    public void waitForInstance(String serviceId) {
        Instant deadline = Instant.now().plus(TIMEOUT);
        log.info("Waiting up to {} for {} to register with Eureka", TIMEOUT, serviceId);
        while (Instant.now().isBefore(deadline)) {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
            if (instances != null && !instances.isEmpty()) {
                log.info("Instance {} registered (host={}, port={})",
                        serviceId,
                        instances.get(0).getHost(),
                        instances.get(0).getPort());
                return;
            }
            sleep(POLL_INTERVAL);
        }
        throw new ProvisioningException(
                ProvisioningException.Code.EUREKA_TIMEOUT,
                serviceId + " did not register within " + TIMEOUT);
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ProvisioningException(
                    ProvisioningException.Code.EUREKA_TIMEOUT,
                    "Readiness probe interrupted");
        }
    }
}