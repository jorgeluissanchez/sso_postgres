package com.co.eurekatic.query.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Externalized datasource configuration. Bound from
 * {@code application.yml} under {@code query.datasources}.
 *
 * <p>One logical group per dialect: {@code postgres},
 * {@code oracle}, {@code sqlserver}. The legacy hard-coded a
 * single datasource; the modernized service supports three
 * because the spec lists each query's target DB on its
 * catalog row.
 *
 * <p><b>Why a Map of {@link Entry} and not a record per
 * dialect:</b> Spring Boot's relaxed binding requires the
 * keys to be known at compile time when using records.
 * A {@code Map<String, Entry>} lets new dialects be added in
 * YAML without code changes.
 *
 * <p><b>Constructor binding:</b> with {@code @ConfigurationProperties}
 * applied to the class, Spring binds the
 * {@code query.datasources.<key>.*} tree directly into the
 * map via the {@link #DataSourceProps(Map)} constructor —
 * no nested {@code entries} key in the YAML.
 *
 * <p><b>Per-dialect flag {@code enabled}:</b> dev runs only
 * Postgres, prod runs all three. Disabled entries are skipped
 * during bean construction so missing driver URLs don't
 * fail-fast on boot.
 */
@ConfigurationProperties(prefix = "query.datasources")
public class DataSourceProps {

    /**
     * Dialect entries keyed by {@code "postgres"},
     * {@code "oracle"}, {@code "sqlserver"}.
     */
    private final Map<String, Entry> entries;

    public DataSourceProps(Map<String, Entry> entries) {
        this.entries = entries != null ? entries : new HashMap<>();
    }

    public Map<String, Entry> getEntries() {
        return entries;
    }

    /**
     * One dialect's connection settings + the names of
     * queries whose catalog row targets this DB.
     */
    public static class Entry {
        /** Master switch. {@code false} skips bean creation. */
        private boolean enabled = true;
        /** JDBC URL. */
        private String url;
        /** Driver class — defaults to one matching the dialect name. */
        private String driverClassName;
        private String username;
        private String password;
        /** Hikari pool size. */
        private int maximumPoolSize = 10;

        @NestedConfigurationProperty
        private HikariProps hikari = new HikariProps();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getDriverClassName() { return driverClassName; }
        public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public int getMaximumPoolSize() { return maximumPoolSize; }
        public void setMaximumPoolSize(int maximumPoolSize) { this.maximumPoolSize = maximumPoolSize; }
        public HikariProps getHikari() { return hikari; }
        public void setHikari(HikariProps hikari) { this.hikari = hikari; }
    }

    /**
     * Hikari tunables exposed via
     * {@code query.datasources.<name>.hikari.*}. Kept
     * separate from {@link Entry} so {@code @NestedConfigurationProperty}
     * works (records don't nest cleanly with relaxed binding).
     */
    public static class HikariProps {
        private long connectionTimeout = 30_000;
        private long idleTimeout = 600_000;
        private long maxLifetime = 1_800_000;
        private int minimumIdle = 2;

        public long getConnectionTimeout() { return connectionTimeout; }
        public void setConnectionTimeout(long connectionTimeout) { this.connectionTimeout = connectionTimeout; }
        public long getIdleTimeout() { return idleTimeout; }
        public void setIdleTimeout(long idleTimeout) { this.idleTimeout = idleTimeout; }
        public long getMaxLifetime() { return maxLifetime; }
        public void setMaxLifetime(long maxLifetime) { this.maxLifetime = maxLifetime; }
        public int getMinimumIdle() { return minimumIdle; }
        public void setMinimumIdle(int minimumIdle) { this.minimumIdle = minimumIdle; }
    }
}