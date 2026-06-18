#!/bin/sh
#
# Postgres init script — sso-admin Phase 2
# (microservice / endpoint / route + their join tables).
#
# Runs after 02-create-sso-admin-tables.sh on first startup
# of the `postgres` container. All statements are idempotent
# (CREATE TABLE IF NOT EXISTS) so re-running is safe.
#
# Table-name convention: we KEEP the legacy UPPER_CASE singular
# names (MICROSERVICE, ENDPOINT, ROUTE) for two reasons:
#   1. The legacy code references these in raw SQL across
#      multiple service classes; an eventual legacy co-existence
#      phase needs the same identifiers.
#   2. Joining tables (ENDPOINT_MICROSERVICE, ROLE_ENDPOINT,
#      ROLE_ROUTE) follow the same pattern and stay aligned.
#
# Note: ROLE_ENDPOINT and ROLE_ROUTE are added here even though
# `role` already exists (from 01-create-sso-db.sh). The legacy
# uses these join tables to gate which roles can invoke a given
# endpoint or see a given route. We do NOT add APP_MICROSERVICE
# or APP_ROUTE — the relational `app` table arrives in Phase 3
# (with App/Screen/Brick) and we'll layer those joins in 04-*.

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL

    -- ========================================================================
    -- MICROSERVICE
    -- ========================================================================
    -- A single downstream HTTP service that the gateway can route to.
    -- serviceId is the natural key used by the gateway to look up routing
    -- info. CREATEDDATE is populated by the DB on insert (default now()).
    CREATE TABLE IF NOT EXISTS MICROSERVICE (
        ID_MICROSERVICE  BIGSERIAL    PRIMARY KEY,
        SERVICEID        VARCHAR(200) NOT NULL UNIQUE,
        DESCRIPTION      VARCHAR(500),
        REQUESTURI       VARCHAR(500),
        TARGETURIPATH    VARCHAR(500),
        TARGETURLHOST    VARCHAR(500),
        TARGETURLPORT    VARCHAR(10),
        CREATEDDATE      TIMESTAMP    NOT NULL DEFAULT now()
    );

    -- ========================================================================
    -- ENDPOINT
    -- ========================================================================
    -- A single HTTP method+path inside a microservice (e.g. GET /api/users).
    -- The legacy enforces uniqueness on (PATH, METHOD, DESCRIPTION); we
    -- mirror that as a real DB-level constraint so concurrent inserts can't
    -- bypass it.
    CREATE TABLE IF NOT EXISTS ENDPOINT (
        ID_ENDPOINT   BIGSERIAL    PRIMARY KEY,
        METHOD        VARCHAR(10)  NOT NULL,
        PATH          VARCHAR(500) NOT NULL,
        DESCRIPTION   VARCHAR(500),
        NUMBERPARAMS  INTEGER      NOT NULL DEFAULT 0,
        CONSTRAINT uq_endpoint_path_method_desc UNIQUE (PATH, METHOD, DESCRIPTION)
    );

    -- ========================================================================
    -- ROUTE
    -- ========================================================================
    -- A navigable menu item in the legacy lowcode platform. Self-referencing
    -- tree via IDPARENT (NULL = root). The legacy stored 0 as the root
    -- sentinel; the modernized service layer normalizes incoming "0" to
    -- NULL on write. COMPONENT_ID_COMPONENT is intentionally OMITTED — it
    -- pointed at a RethinkDB-only table that does not exist relationally.
    CREATE TABLE IF NOT EXISTS ROUTE (
        ID_ROUTE   BIGSERIAL    PRIMARY KEY,
        NAME       VARCHAR(200) NOT NULL,
        ICON       VARCHAR(200),
        PATH       VARCHAR(500) NOT NULL,
        MENUORDER  INTEGER      NOT NULL DEFAULT 0,
        TYPE       VARCHAR(50),
        IDPARENT   BIGINT       REFERENCES ROUTE(ID_ROUTE) ON DELETE CASCADE
    );

    -- ========================================================================
    -- ENDPOINT_MICROSERVICE (M:N)
    -- ========================================================================
    -- Owning side: Endpoint (common/.../entity/Endpoint.java).
    CREATE TABLE IF NOT EXISTS ENDPOINT_MICROSERVICE (
        ENDPOINT_ID     BIGINT NOT NULL REFERENCES ENDPOINT(ID_ENDPOINT)       ON DELETE CASCADE,
        MICROSERVICE_ID BIGINT NOT NULL REFERENCES MICROSERVICE(ID_MICROSERVICE) ON DELETE CASCADE,
        PRIMARY KEY (ENDPOINT_ID, MICROSERVICE_ID)
    );

    -- ========================================================================
    -- ROLE_ENDPOINT (M:N)
    -- ========================================================================
    -- Owning side: Endpoint. Gates which roles can invoke a given endpoint.
    CREATE TABLE IF NOT EXISTS ROLE_ENDPOINT (
        ENDPOINT_ID BIGINT NOT NULL REFERENCES ENDPOINT(ID_ENDPOINT) ON DELETE CASCADE,
        ROLE_ID     BIGINT NOT NULL REFERENCES role(id_role)         ON DELETE CASCADE,
        PRIMARY KEY (ENDPOINT_ID, ROLE_ID)
    );

    -- ========================================================================
    -- ROLE_ROUTE (M:N)
    -- ========================================================================
    -- Owning side: Route. Gates which roles can see a given menu route.
    CREATE TABLE IF NOT EXISTS ROLE_ROUTE (
        ROUTE_ID BIGINT NOT NULL REFERENCES ROUTE(ID_ROUTE) ON DELETE CASCADE,
        ROLE_ID  BIGINT NOT NULL REFERENCES role(id_role)   ON DELETE CASCADE,
        PRIMARY KEY (ROUTE_ID, ROLE_ID)
    );

    -- Helpful indexes for the most common lookups.
    CREATE INDEX IF NOT EXISTS idx_microservice_serviceid ON MICROSERVICE(SERVICEID);
    CREATE INDEX IF NOT EXISTS idx_endpoint_path          ON ENDPOINT(PATH);
    CREATE INDEX IF NOT EXISTS idx_route_idparent        ON ROUTE(IDPARENT);
    CREATE INDEX IF NOT EXISTS idx_route_name_path        ON ROUTE(NAME, PATH);

EOSQL

echo "✓ sso-admin (Phase 2) schema created: MICROSERVICE, ENDPOINT, ROUTE, ENDPOINT_MICROSERVICE, ROLE_ENDPOINT, ROLE_ROUTE"
