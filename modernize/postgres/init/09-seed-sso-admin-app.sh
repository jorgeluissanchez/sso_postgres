#!/bin/sh
#
# Postgres init script — sso-admin sidebar App grouping seed.
#
# Complements 08-seed-sso-admin-routes.sh. That script bound
# every /admin/** route to the ADMIN role via the direct
# ROLE_ROUTE join table. This script populates the SECOND
# branch of MyMenuController's findVisibleForRoles union
# query (RouteRepository.findVisibleForRoles in
# common/.../repository/RouteRepository.java): the App path.
#
#   SELECT r2.id FROM App a2 JOIN a2.routes r2 JOIN a2.roles rol2
#    WHERE rol2.id IN :roleIds
#
# app_route (App.routes) is the SOLE app-membership relationship
# for a route — ROUTE has no id_app FK of its own (removed; a
# route can legitimately belong to more than one app, which a
# singular FK couldn't represent, and it drifted out of sync
# with app_route whenever an admin bound/unbound via the App
# edit page's route picker instead).
#
# Two rows are mutated:
#   1. role_app  — bind ADMIN to the SSO-ADMIN app.
#   2. app_route — populate the M:N between the SSO-ADMIN app
#                  and every /admin/** route.
#
# The SSO-ADMIN app row was pre-created as a placeholder
# shell (name 'SSO-ADMIN', id 6, no bindings). We populate
# it in place — creating a new 'sso-admin' lowercase app
# in addition would leave the SSO-ADMIN shell dangling
# forever. The naming stays uppercase for consistency with
# the rest of the data.
#
# Every statement is idempotent:
#   - role_app uses ON CONFLICT (id_app, id_role) DO NOTHING
#   - app_route uses ON CONFLICT (id_app, id_route) DO NOTHING
#
# Result: an admin calling GET /sso-admin/myMenu still
# receives the same 13 routes (12 seeded + 1 pre-existing
# /admin/reports), but the data model is now complete —
# both union branches of findVisibleForRoles exercise the
# same set of rows.

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-'EOSQL'

    -- ========================================================================
    -- 1. role_app — ADMIN -> SSO-ADMIN
    -- ========================================================================
    INSERT INTO role_app (id_app, id_role)
    SELECT a.id_app, r.id_role
    FROM app a
    CROSS JOIN role r
    WHERE a.name = 'SSO-ADMIN'
      AND r.name = 'ADMIN'
    ON CONFLICT (id_app, id_role) DO NOTHING;

    -- ========================================================================
    -- 2. app_route — fill the M:N for SSO-ADMIN
    -- ========================================================================
    -- Every /admin/** route joins SSO-ADMIN's app_route set
    -- directly by path pattern — no id_app FK involved.
    INSERT INTO app_route (id_app, id_route)
    SELECT a.id_app, ro.ID_ROUTE
    FROM app a
    CROSS JOIN ROUTE ro
    WHERE a.name = 'SSO-ADMIN'
      AND ro.PATH LIKE '/admin/%'
    ON CONFLICT (id_app, id_route) DO NOTHING;

EOSQL

echo "✓ sso-admin App grouping seeded (ADMIN bound to SSO-ADMIN; all /admin/** routes linked)"
