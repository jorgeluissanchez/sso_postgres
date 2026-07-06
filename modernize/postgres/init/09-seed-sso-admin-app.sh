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
#   SELECT r2.id FROM Route r2
#     WHERE r2.app IS NOT NULL
#       AND r2.app.id IN (
#           SELECT ra.id FROM App ra JOIN ra.roles rol
#            WHERE rol.id IN :roleIds
#       )
#
# Three rows are mutated:
#   1. role_app     — bind ADMIN to the SSO-ADMIN app.
#   2. ROUTE.id_app  — point every /admin/** route at the
#                      SSO-ADMIN app (only routes whose
#                      id_app was NULL are touched; pre-
#                      existing bindings like Usuarios→
#                      ColombiaEvaluadora are preserved).
#   3. app_route    — populate the M:N between the SSO-ADMIN
#                     app and every /admin/** route (or
#                     more precisely, every route whose
#                     id_app is now SSO-ADMIN).
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
#   - ROUTE.id_app UPDATE is gated by id_app IS NULL so we
#     never overwrite an intentional admin-set binding
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
    -- 2. ROUTE.id_app — every /admin/** route into SSO-ADMIN
    -- ========================================================================
    -- Only touches rows where id_app IS NULL — pre-existing
    -- manual bindings (e.g. Usuarios→ColombiaEvaluadora
    -- created by an earlier smoke run) survive. Those
    -- routes are still visible to ADMIN via the direct
    -- ROLE_ROUTE branch + the now-also-app-bound Colombia-
    -- Evaluadora branch, so nothing in /myMenu regresses.
    UPDATE ROUTE
       SET id_app = (SELECT id_app FROM app WHERE name = 'SSO-ADMIN')
     WHERE PATH LIKE '/admin/%'
       AND id_app IS NULL;

    -- ========================================================================
    -- 3. app_route — fill the M:N for SSO-ADMIN
    -- ========================================================================
    -- Idempotent: a route may already be linked to
    -- SSO-ADMIN via the previous UPDATE setting id_app —
    -- we still insert the explicit app_route row so the
    -- AppController.getRoutesChecked call has the row to
    -- report.
    INSERT INTO app_route (id_app, id_route)
    SELECT a.id_app, ro.ID_ROUTE
    FROM app a
    CROSS JOIN ROUTE ro
    WHERE a.name = 'SSO-ADMIN'
      AND ro.PATH LIKE '/admin/%'
      AND ro.id_app = a.id_app
    ON CONFLICT (id_app, id_route) DO NOTHING;

EOSQL

echo "✓ sso-admin App grouping seeded (ADMIN bound to SSO-ADMIN; all /admin/** routes linked)"
