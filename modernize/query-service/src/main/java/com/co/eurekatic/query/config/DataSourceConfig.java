package com.co.eurekatic.query.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds one {@link HikariDataSource} per configured dialect
 * plus a {@link NamedParameterJdbcTemplate} per dialect.
 *
 * <p>Why we don't use {@code @ConfigurationProperties}
 * auto-configured DataSource: the legacy needed three
 * simultaneously-active pools and a router that picks one
 * based on the resolved query's target dialect. Spring Boot's
 * default {@code spring.datasource.*} only knows about a
 * single primary DataSource, and
 * {@code AbstractRoutingDataSource} requires all candidates
 * at startup.
 *
 * <p>Disabled dialects (entry.enabled=false) are simply not
 * registered — the bean list is dynamic. The resolver
 * ({@code JdbcTemplateRegistry}) reads the same
 * {@link DataSourceProps} and maps {@code "postgres"} to
 * the Postgres JdbcTemplate, etc. Dialects that aren't
 * registered throw at resolve time, not at boot time, which
 * is the desired behavior for dev (only Postgres configured).
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    /**
     * Default driver per dialect key — overridable via
     * {@code query.datasources.<name>.driver-class-name}.
     */
    private static final Map<String, String> DEFAULT_DRIVERS = Map.of(
            "postgres", "org.postgresql.Driver",
            "oracle", "oracle.jdbc.OracleDriver",
            "sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver"
    );

    /**
     * All enabled datasources keyed by dialect name. Useful
     * for the {@code Actuator} health endpoint (one health
     * indicator per pool).
     *
     * <p>The {@code DataSourceAutoConfiguration} (and
     * {@code JdbcTemplateAutoConfiguration}) are excluded by
     * the application bootstrap. We own pool creation here
     * and surface a {@code Map<String, DataSource>} keyed by
     * dialect name so the read/write paths can pick the
     * right one based on the catalog row's {@code type}
     * column.
     */
    @Bean(name = "queryDataSources")
    public Map<String, DataSource> dataSources(DataSourceProps props) {
        Map<String, DataSource> sources = new LinkedHashMap<>();
        props.getEntries().forEach((name, entry) -> {
            if (!entry.isEnabled()) {
                log.info("Datasource '{}' is disabled — skipping bean", name);
                return;
            }
            String driver = entry.getDriverClassName() != null
                    ? entry.getDriverClassName()
                    : DEFAULT_DRIVERS.get(name);
            if (driver == null) {
                throw new IllegalStateException(
                        "Datasource '" + name + "' has no driver-class-name and no default. "
                                + "Known defaults: " + DEFAULT_DRIVERS.keySet());
            }
            HikariDataSource ds = DataSourceBuilder.create()
                    .type(HikariDataSource.class)
                    .driverClassName(driver)
                    .url(entry.getUrl())
                    .username(entry.getUsername())
                    .password(entry.getPassword())
                    .build();
            ds.setPoolName("query-" + name);
            ds.setMaximumPoolSize(entry.getMaximumPoolSize());
            ds.setConnectionTimeout(entry.getHikari().getConnectionTimeout());
            ds.setIdleTimeout(entry.getHikari().getIdleTimeout());
            ds.setMaxLifetime(entry.getHikari().getMaxLifetime());
            ds.setMinimumIdle(entry.getHikari().getMinimumIdle());
            sources.put(name, ds);
            log.info("Datasource '{}' ready: pool={}, max={}, url={}",
                    name, "query-" + name, entry.getMaximumPoolSize(),
                    redact(entry.getUrl()));
        });
        return sources;
    }

    /**
     * One {@link NamedParameterJdbcTemplate} per dialect.
     * Named parameter binding is preferred over positional
     * because the catalog query strings use {@code :param}
     * placeholders and admin authors expect that semantics.
     */
    @Bean(name = "queryJdbcTemplates")
    public Map<String, NamedParameterJdbcTemplate> jdbcTemplates(
            @org.springframework.beans.factory.annotation.Qualifier("queryDataSources")
                    Map<String, DataSource> dataSources) {
        Map<String, NamedParameterJdbcTemplate> templates = new HashMap<>();
        dataSources.forEach((name, ds) ->
                templates.put(name, new NamedParameterJdbcTemplate(ds)));
        return templates;
    }

    private static String redact(String url) {
        // Strip credentials if embedded in the JDBC URL
        // (Oracle URLs sometimes carry them).
        int q = url.indexOf('?');
        return q < 0 ? url : url.substring(0, q) + "?...";
    }
}