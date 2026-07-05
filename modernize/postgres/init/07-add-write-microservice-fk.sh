#!/usr/bin/env bash
# =============================================================================
# 07-add-write-microservice-fk.sh
#
# Adds the MICROSERVICE_ID FK on WRITE_DEFINITION that mirrors the
# pattern already in place for QUERY (see 05-create-query-tables.sh
# lines 27-50). The admin-ui Writes Catalog needs the binding so
# the form can offer a "pick a table from the connected datasource"
# dropdown per backing instance.
#
# Why a separate script and not 06-…: the writes table existed
# already without the FK. Adding the column in place preserves
# `git blame` on the original create (05-) while making this
# migration a focused, dated change — easier to roll back if
# Hibernate's validate rejects the new column for some reason
# (driver version, missing microservice row, …) and easier for
# operators to spot in `start-local.sh` output.
#
# Idempotent: `ADD COLUMN IF NOT EXISTS` (Postgres ≥ 9.6) +
# `CREATE INDEX IF NOT EXISTS` + the backfill `UPDATE` is a
# no-op when the FK already exists. Re-running this script
# against a DB that already has the column is safe.
#
# Backfill mirrors lines 99-109 of 05-create-query-tables.sh:
# orphan writes (no MICROSERVICE_ID) get pointed at the legacy
# canonical `query-service-postgres` instance if it exists.
# Rows left as NULL become the new "global" semantic (any QUERY
# instance with the right datasource may serve them), which is
# what the admin-ui renders as "Sin binding (global)".
# =============================================================================

set -e

psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER}" --dbname "${POSTGRES_DB}" <<-'EOSQL'

-- Same FK shape as QUERY: nullable BIGINT → MICROSERVICE(ID_MICROSERVICE).
-- The MICROSERVICE PK column is `id_microservice` (legacy), NOT `id` —
-- referencing the wrong column would make Hibernate's ddl-auto=validate
-- reject the FK on sso-admin boot. Keep this column name in sync with
-- 02-create-sso-admin-tables.sh (which created MICROSERVICE) and with
-- the Query entity's @JoinColumn(name = "MICROSERVICE_ID").
ALTER TABLE WRITE_DEFINITION
    ADD COLUMN IF NOT EXISTS MICROSERVICE_ID BIGINT
        REFERENCES MICROSERVICE(ID_MICROSERVICE);

CREATE INDEX IF NOT EXISTS idx_write_definition_microservice
    ON WRITE_DEFINITION(MICROSERVICE_ID);

-- Defensive backfill: point existing orphan writes at the legacy
-- canonical `query-service-postgres` instance so the previous
-- single-instance setup keeps working without an admin re-binding
-- every row. Same shape as 05-create-query-tables.sh:99-109.
UPDATE WRITE_DEFINITION w
   SET MICROSERVICE_ID = (SELECT ID_MICROSERVICE FROM MICROSERVICE
                          WHERE KIND = 'QUERY' AND INSTANCENAME = 'postgres'
                          LIMIT 1)
 WHERE w.MICROSERVICE_ID IS NULL
   AND EXISTS (SELECT 1 FROM MICROSERVICE
                WHERE KIND = 'QUERY' AND INSTANCENAME = 'postgres');

EOSQL
