#!/bin/sh
#
# Postgres init script — sso-admin Phase 1 (groups / user_group).
# Adds the sso-admin metadata tables on top of the auth tables
# created by 01-create-sso-db.sh. Runs once on first startup of
# the `postgres` container, after 01-*.
#
# The official postgres image runs all *.sh files in
# /docker-entrypoint-initdb.d/ in alphabetical order. Future
# phases (microservice/endpoint/route in 03-, app/screen/brick
# in 04-, etc.) can layer as 03-*, 04-*, etc.

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL

    -- GROUPS: a named collection of users, used for coarse-grained
    -- access scoping. Distinct from ROLE (which is per-user, fine
    -- grained). The modernized stack uses this for grouping users
    -- (e.g. "operators of company X") and binding groups to apps.
    --
    -- Schema mirrors the legacy GROUPS table from sso-service.
    CREATE TABLE IF NOT EXISTS groups (
        id_group    BIGSERIAL    PRIMARY KEY,
        name        VARCHAR(120) NOT NULL UNIQUE,
        description VARCHAR(255)
    );

    -- user_group: M:N between users and groups. The owning side
    -- of the relationship is the Group entity (see
    -- common/.../entity/Group.java) — JPA reads/writes via the
    -- groups side.
    CREATE TABLE IF NOT EXISTS user_group (
        group_id BIGINT NOT NULL REFERENCES groups(id_group) ON DELETE CASCADE,
        user_id  BIGINT NOT NULL REFERENCES users(id_user)  ON DELETE CASCADE,
        PRIMARY KEY (group_id, user_id)
    );

    -- Helpful indexes for the lookups sso-admin does.
    CREATE INDEX IF NOT EXISTS idx_groups_name ON groups(name);

EOSQL

echo "✓ sso-admin (Phase 1) schema created: groups, user_group"

