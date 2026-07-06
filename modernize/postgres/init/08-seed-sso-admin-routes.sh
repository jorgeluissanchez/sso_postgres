#!/bin/sh
#
# Postgres init script — sso-admin sidebar menu seed.
#
# Inserts the twelve `ROUTE` rows that the legacy hardcoded
# Sidebar.tsx rendered before the dynamic sidebar was wired to
# GET /sso-admin/myMenu (commits 0f5b03d / 22aa6ef / da92221
# / 62a0470 / 74626b4 / d7e7412). With this seed in place an
# ADMIN caller sees the full sidebar — twenty years of muscle
# memory carries over to the new SPA.
#
# The script is idempotent:
#   - The ROUTE insert uses a VALUES table + WHERE NOT EXISTS
#     on PATH, so re-running against an already-seeded DB is a
#     no-op.
#   - The ROLE_ROUTE insert uses the composite PK +
#     ON CONFLICT DO NOTHING so duplicate bindings can't
#     accumulate.
#
# Authorization wiring: the seed binds every route directly
# to the ADMIN role via ROLE_ROUTE. The alternative path
# through App/role_app/app_route is intentionally NOT seeded
# here — the Sidebar test (smoke-account-lifecycle + a new
# smoke-myMenu check in a follow-up) wants the simplest data
# path that exercises RouteRepository.findVisibleForRoles
# via the UNION second branch. Direct ROLE_ROUTE binding is
# also the documented mechanism for ad-hoc route grants
# independent of app membership.
#
# Icon names map to lucide-react symbols shipped by the SPA:
#   users / shieldcheck / box / database / layers / settings
#   foldertree / network / filetext
# If a name doesn't resolve in the SPA's curated icon map
# (admin-ui/src/layout/Sidebar.tsx resolveIcon), the Spanish-
# label keyword fallback kicks in; the final fallback is a
# neutral Circle glyph so the rendered cell is never empty.
#
# Numbers start at 1 and grow by 1; menuOrder is the
# tiebreaker after Path for any future custom-sorted
# categories.

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-'EOSQL'

    -- ========================================================================
    -- ROUTE rows (the legacy twelve)
    -- ========================================================================
    INSERT INTO ROUTE (NAME, ICON, PATH, MENUORDER, TYPE, IDPARENT)
    SELECT v.name, v.icon, v.path, v.menu_order, NULL, NULL
    FROM (VALUES
        ('Usuarios',         'users',       '/admin/users',          1),
        ('Roles',            'shieldcheck', '/admin/roles',          2),
        ('Grupos',           'users',       '/admin/groups',         3),
        ('Microservicios',   'box',         '/admin/microservices',  4),
        ('Query Services',   'database',    '/admin/query-services', 5),
        ('Queries Catalog',  'layers',      '/admin/queries',        6),
        ('Queries (admin)',  'settings',    '/admin/query-catalog',  7),
        ('Dynamic CRUD',     'foldertree',  '/admin/dynamic-crud',   8),
        ('Endpoints',        'network',     '/admin/endpoints',      9),
        ('Rutas',            'network',     '/admin/routes',        10),
        ('Apps',             'layers',      '/admin/apps',          11),
        ('Writes',           'filetext',    '/admin/writes',        12)
    ) AS v(name, icon, path, menu_order)
    WHERE NOT EXISTS (
        SELECT 1 FROM ROUTE WHERE ROUTE.PATH = v.path
    );

    -- ========================================================================
    -- ROLE_ROUTE bindings — ADMIN gets all twelve
    -- ========================================================================
    -- Cross-join on the role name (looked up from
    -- DataInitializer's "ADMIN" / "Administrator" pair) plus
    -- a path LIKE predicate scoped to the SPA so we never
    -- bind a route that doesn't match /admin/** if the
    -- seed is reused on a future-stamped DB that has more
    -- routes than the legacy twelve.
    --
    -- ON CONFLICT (ROLE_ID, ROUTE_ID) DO NOTHING makes this
    -- safe to re-run: the composite PK is the conflict target.
    INSERT INTO ROLE_ROUTE (ROLE_ID, ROUTE_ID)
    SELECT r.id_role, ro.ID_ROUTE
    FROM role r
    CROSS JOIN ROUTE ro
    WHERE r.name = 'ADMIN'
      AND ro.PATH LIKE '/admin/%'
    ON CONFLICT (ROLE_ID, ROUTE_ID) DO NOTHING;

EOSQL

echo "✓ sso-admin sidebar menu seeded (12 routes bound to ADMIN)"
