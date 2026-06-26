#!/usr/bin/env bash
# =============================================================================
# 04-add-microservice-query-fields.sh
#
# Adds the columns needed by the dynamic query-service
# provisioner (admin-ui "kind=QUERY" microservice rows):
#
#   KIND           — discriminator ('REST' default, 'QUERY' for
#                    docker-provisioned query-service instances).
#   DIALECT        — postgres | oracle | sqlserver. NULL for
#                    REST rows.
#   JDBCURL        — connection string the new container gets
#                    via QUERY_DS_URL. NULL for REST rows.
#   DBUSERNAME     — DB user.
#   DBPASSWORD     — DB password. Stored plaintext in v1 — see
#                    TODO in MicroserviceService for the prod
#                    encryption follow-up.
#   POOLSIZE       — HikariCP maximum-pool-size.
#   INSTANCENAME   — overrides the dialect-derived service id
#                    ("query-service-<INSTANCENAME>"). NULL for
#                    REST rows.
#
# Idempotent — every DDL uses IF NOT EXISTS / IF NULL so
# `docker compose down -v && up` works without manual cleanup.
# =============================================================================

set -e

psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER}" --dbname "${POSTGRES_DB}" <<-'EOSQL'

ALTER TABLE MICROSERVICE
    ADD COLUMN IF NOT EXISTS KIND VARCHAR(20) NOT NULL DEFAULT 'REST';

ALTER TABLE MICROSERVICE
    ADD COLUMN IF NOT EXISTS DIALECT VARCHAR(40);

ALTER TABLE MICROSERVICE
    ADD COLUMN IF NOT EXISTS JDBCURL VARCHAR(1000);

ALTER TABLE MICROSERVICE
    ADD COLUMN IF NOT EXISTS DBUSERNAME VARCHAR(120);

ALTER TABLE MICROSERVICE
    ADD COLUMN IF NOT EXISTS DBPASSWORD VARCHAR(500);

ALTER TABLE MICROSERVICE
    ADD COLUMN IF NOT EXISTS POOLSIZE INTEGER;

ALTER TABLE MICROSERVICE
    ADD COLUMN IF NOT EXISTS INSTANCENAME VARCHAR(120);

-- Partial unique index: instanceName is non-null only for
-- QUERY rows; REST rows can coexist freely.
CREATE UNIQUE INDEX IF NOT EXISTS uq_microservice_instancename
    ON MICROSERVICE(INSTANCENAME) WHERE INSTANCENAME IS NOT NULL;

-- Defensive backfill for rows inserted before the column
-- existed (legacy CREATE TABLE never set it; the default
-- already handles new rows but old rows in down-graded
-- scenarios would still be NULL).
UPDATE MICROSERVICE SET KIND = 'REST' WHERE KIND IS NULL;

EOSQL