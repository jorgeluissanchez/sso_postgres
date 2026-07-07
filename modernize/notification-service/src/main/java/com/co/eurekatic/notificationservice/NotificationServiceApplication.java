package com.co.eurekatic.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for notification-service.
 *
 * <p>The application is a Spring Boot 4.0 servlet app that:
 * <ul>
 *   <li>registers with Eureka ({@code @EnableDiscoveryClient}),</li>
 *   <li>consumes JSON envelopes from three RabbitMQ queues
 *       (sms / email / push) via {@code @RabbitListener},</li>
 *   <li>persists {@code notification_log} rows for idempotency
 *       and audit,</li>
 *   <li>re-renders templates and dispatches to the configured
 *       provider(s) with Resilience4j circuit breakers,</li>
 *   <li>refreshes its provider configuration every 30 s
 *       ({@code @EnableScheduling}) and exposes the effective
 *       config + breaker state via {@code /actuator/providers}.</li>
 * </ul>
 *
 * <p>{@code common}'s entities, repositories and security helpers
 * live outside this app's default base package, and this app has
 * its own {@code repository} subpackage (e.g.
 * {@code NotificationLogRepository}), so {@code ComponentScan},
 * {@code EntityScan} and {@code EnableJpaRepositories} all have to
 * point at the right packages explicitly — same pattern as
 * {@code AuthCenterApplication}.
 *
 * <p><b>Spring Boot 4 migration:</b> {@code EntityScan} moved from
 * {@code org.springframework.boot.autoconfigure.domain} to
 * {@code org.springframework.boot.persistence.autoconfigure} as part
 * of the per-stack split in Boot 4.0.x. The class itself is
 * unchanged.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@ComponentScan(basePackages = {
        "com.co.eurekatic.notificationservice",
        "com.co.eurekatic.common.entity",
        "com.co.eurekatic.common.repository",
        "com.co.eurekatic.common.security"
})
@EntityScan(basePackages = {
        // NotificationLog lives in this module's `domain`
        // package; scanning the whole module subtree catches it
        // and any future entities without re-touching this list.
        // JpaRepository interfaces get filtered out automatically.
        "com.co.eurekatic.notificationservice",
        "com.co.eurekatic.common.entity"
})
@EnableJpaRepositories(basePackages = {
        // ProviderConfigRepository actually lives in the
        // `provider` subpackage, not a dedicated `repository`
        // one — scanning the whole module subtree catches both.
        "com.co.eurekatic.notificationservice",
        "com.co.eurekatic.common.repository"
})
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
