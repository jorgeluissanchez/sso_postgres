package com.co.eurekatic.query.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Rewrites {@code spring.application.name} early in the
 * Boot lifecycle so the instance registers with Eureka
 * under a per-dialect service-id in scale-out deployments.
 *
 * <p>Logic:
 * <ul>
 *   <li>If {@code QUERY_INSTANCE_NAME} is set (the explicit
 *       override), use it as-is.</li>
 *   <li>Else if {@code QUERY_DS_DIALECT} is set, derive
 *       {@code query-service-<dialect>}.</li>
 *   <li>Else leave the YAML value untouched (single-instance
 *       / dev mode).</li>
 * </ul>
 *
 * <p>Why an {@link EnvironmentPostProcessor} instead of a
 * regular bean: the application name has to be set BEFORE
 * Spring Cloud's Eureka client builds its registration
 * payload (which reads {@code spring.application.name}
 * during context refresh). A regular
 * {@code @Configuration} bean runs too late.
 *
 * <p><b>Why HIHEST_PRECEDENCE order:</b> this needs to run
 * before the {@code ConfigFileApplicationListener} so the
 * YAML-derived name (if any) gets overwritten by the env
 * var. Otherwise the YAML wins and every container ends up
 * as {@code query-service} regardless of the dialect it
 * serves.
 */
public class InstanceNameResolver implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env,
                                       SpringApplication application) {
        String explicitName = env.getProperty("query.instance.name");
        String dialect = env.getProperty("query.ds.dialect");
        if (explicitName == null || explicitName.isBlank()) {
            if (dialect == null || dialect.isBlank()) {
                // No instance info → leave it alone. Static
                // mode keeps its YAML name.
                return;
            }
            explicitName = "query-service-" + dialect.toLowerCase(Locale.ROOT);
        }

        Map<String, Object> overrides = new HashMap<>();
        overrides.put("spring.application.name", explicitName);
        env.getPropertySources().addFirst(
                new MapPropertySource("query-instance-name", overrides));
        // Make sure the value is visible via @Value etc.
        // (addFirst already gives it highest precedence.)
    }

    @Override
    public int getOrder() {
        // Run before ConfigFileApplicationListener (which
        // is HIGHEST_PRECEDENCE + 10) so we can win over
        // the YAML value if needed.
        return Ordered.HIGHEST_PRECEDENCE;
    }
}