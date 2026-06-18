#!/bin/sh
#
# Postgres init script — runs once on first startup of the
# `postgres` container, before the database is ready to accept
# connections. The official postgres image mounts *.sh and *.sql
# files in /docker-entrypoint-initdb.d/ and runs them in
# alphabetical order.
#
# This script:
#   1. Creates the `sso` database (separate from the default
#      `postgres` db).
#   2. Creates the `users`, `role`, and `role_users` tables that
#      mirror the legacy schema. auth-center runs with
#      `spring.jpa.hibernate.ddl-auto=validate`, so the schema
#      MUST exist before it starts.

set -e

# `psql -v ON_ERROR_STOP=1` makes the script fail loudly on any
# SQL error, so partial schema isn't a thing.
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL

    -- The legacy sso_postgres schema names its tables `users` and
    -- `role` (which is a reserved word in SQL standard, hence the
    -- quoted identifier). Hibernate maps to these names via the
    -- @Table / @Column annotations on the entities.
    CREATE TABLE IF NOT EXISTS users (
        id_user                 BIGSERIAL    PRIMARY KEY,
        username                VARCHAR(80)  NOT NULL UNIQUE,
        full_name               VARCHAR(200),
        email                   VARCHAR(200),
        password                VARCHAR(68),
        enabled                 BOOLEAN      NOT NULL DEFAULT TRUE,
        account_non_expired     BOOLEAN      NOT NULL DEFAULT TRUE,
        account_non_locked      BOOLEAN      NOT NULL DEFAULT TRUE,
        credentials_non_expired BOOLEAN      NOT NULL DEFAULT TRUE,
        active                  BOOLEAN      NOT NULL DEFAULT TRUE,
        ldap                    BOOLEAN      NOT NULL DEFAULT FALSE,
        refresh_token           VARCHAR(64),
        api_token               VARCHAR(64),
        token_activation        VARCHAR(64),
        token_restore           VARCHAR(64)
    );

    CREATE TABLE IF NOT EXISTS role (
        id_role     BIGSERIAL    PRIMARY KEY,
        name        VARCHAR(60)  NOT NULL UNIQUE,
        description VARCHAR(200)
    );

    CREATE TABLE IF NOT EXISTS role_users (
        user_id BIGINT NOT NULL REFERENCES users(id_user)  ON DELETE CASCADE,
        role_id BIGINT NOT NULL REFERENCES role(id_role)   ON DELETE CASCADE,
        PRIMARY KEY (user_id, role_id)
    );

    -- Helpful indexes for the lookups auth-center does on every
    -- request (loadUserByUsername).
    CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

EOSQL

echo "✓ sso database schema created"
