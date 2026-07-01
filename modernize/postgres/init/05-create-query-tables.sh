#!/usr/bin/env bash
# =============================================================================
# 05-create-query-tables.sh
#
# Creates the QUERY / ROLE_QUERY / WRITE tables that the JPA entities
# in `common/.../entity/` (Query, Write) and the M:M join expect.
#
# Hibernate is set to `ddl-auto=validate` in sso-admin — every column
# referenced by an @Entity/@Column must exist BEFORE sso-admin boots,
# or the context fails to start. This script ships the schema so the
# code can validate against a real Postgres.
#
# Also adds MICROSERVICE_ID FK on QUERY (and WRITE) so the admin-ui
# Queries Catalog page can group queries by backing instance.
#
# Idempotent — every DDL uses IF NOT EXISTS so `docker compose down -v
# && up` works without manual cleanup, and re-running against a DB that
# already has these tables is a no-op.
# =============================================================================

set -e

psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER}" --dbname "${POSTGRES_DB}" <<-'EOSQL'

-- QUERY — catalog of read SQL templates. Resolved by uuid via
-- sso-admin's GET /getQuery, executed by query-service.
CREATE TABLE IF NOT EXISTS QUERY (
    ID_QUERY        BIGSERIAL PRIMARY KEY,
    UUID            VARCHAR(64)  NOT NULL UNIQUE,
    QUERY           TEXT         NOT NULL,
    TYPE            VARCHAR(64),
    PUBLIC_END      BOOLEAN      NOT NULL DEFAULT FALSE,
    CAPTCHA         BOOLEAN      NOT NULL DEFAULT FALSE,
    DETAIL          TEXT,
    ACTION          TEXT,
    STYLE           TEXT,
    CREATEDDATE     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    -- FK to MICROSERVICE: which query-service instance owns
    -- this query. NULL means "global" — any instance with the
    -- right datasource may serve it (rare; admin UI treats
    -- NULL as "Todas las instancias"). Note: MICROSERVICE's
    -- PK is `id_microservice` (legacy column name preserved by
    -- 02-create-sso-admin-tables.sh), NOT `id` — referencing
    -- the wrong column makes Hibernate's schema validation
    -- reject the FK on sso-admin boot.
    MICROSERVICE_ID BIGINT REFERENCES MICROSERVICE(ID_MICROSERVICE)
);

CREATE INDEX IF NOT EXISTS idx_query_microservice
    ON QUERY(MICROSERVICE_ID);

-- ROLE_QUERY — M:M join between QUERY and ROLE. A user with
-- at least one role in this set (OR PUBLIC_END=TRUE) is
-- authorized to resolve the query through the catalog.
-- Column naming follows the sibling role_route/role_endpoint
-- convention (`<entity>_id`, lowercase snake_case) — the
-- Query entity declares `@JoinTable(joinColumns=@JoinColumn(name="QUERY_ID"))`,
-- and Postgres lowercases unquoted identifiers, so the actual
-- columns land as `query_id` / `role_id`. A column-name mismatch
-- fails Hibernate's `ddl-auto=validate` on sso-admin boot.
CREATE TABLE IF NOT EXISTS ROLE_QUERY (
    QUERY_ID BIGINT NOT NULL REFERENCES QUERY(ID_QUERY) ON DELETE CASCADE,
    ROLE_ID  BIGINT NOT NULL REFERENCES ROLE(ID_ROLE)  ON DELETE CASCADE,
    PRIMARY KEY (QUERY_ID, ROLE_ID)
);

-- WRITE_DEFINITION — write-side counterpart of QUERY.
-- Mirrors the legacy REPORT/UPDATE_DEFINITION columns the
-- query-service WriteService consumes. Per spec, table and
-- column names live ONLY in this catalog row (never on the
-- request body) — callers send a uuid + value map; the
-- service validates keys against COLUMNS, then parameter-
-- binds values. Schema is bound to the WriteDefinition JPA
-- entity (@Table(name = "WRITE_DEFINITION")) — column names
-- match @Column(name=...) verbatim (Postgres lowercases
-- unquoted identifiers, so we declare them as written).
CREATE TABLE IF NOT EXISTS WRITE_DEFINITION (
    ID_WRITE_DEFINITION BIGSERIAL PRIMARY KEY,
    UUID                VARCHAR(64)  NOT NULL UNIQUE,
    WRITE_TYPE          VARCHAR(16)  NOT NULL,
    TABLE_NAME          VARCHAR(200) NOT NULL,
    COLUMNS             TEXT         NOT NULL,
    KEY_COLUMNS         TEXT,
    CREATEDDATE         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ROLE_WRITE — M:M join between WRITE_DEFINITION and ROLE.
-- Same naming convention as role_query (QUERY_ID, ROLE_ID)
-- and the sibling role_route/role_endpoint tables — column
-- names are the lowercase snake_case that Postgres produces
-- from unquoted UPPER_CASE identifiers, which matches the
-- @JoinColumn(name = "WRITE_DEFINITION_ID") on the entity.
CREATE TABLE IF NOT EXISTS ROLE_WRITE (
    WRITE_DEFINITION_ID BIGINT NOT NULL REFERENCES WRITE_DEFINITION(ID_WRITE_DEFINITION) ON DELETE CASCADE,
    ROLE_ID             BIGINT NOT NULL REFERENCES ROLE(ID_ROLE) ON DELETE CASCADE,
    PRIMARY KEY (WRITE_DEFINITION_ID, ROLE_ID)
);

-- Defensive backfill: bind orphan QUERY rows (no MICROSERVICE_ID)
-- to the legacy canonical `query-service-postgres` instance if
-- it exists. This makes the previous single-instance setup
-- keep working without an admin re-binding every row.
UPDATE QUERY q
   SET MICROSERVICE_ID = (SELECT ID_MICROSERVICE FROM MICROSERVICE
                          WHERE KIND = 'QUERY' AND INSTANCENAME = 'postgres'
                          LIMIT 1)
 WHERE q.MICROSERVICE_ID IS NULL
   AND EXISTS (SELECT 1 FROM MICROSERVICE
                WHERE KIND = 'QUERY' AND INSTANCENAME = 'postgres');

EOSQL