#!/usr/bin/env bash
# =============================================================================
# scripts/smoke-myMenu.sh
#
# Pins the dynamic sidebar contract at the live-stack layer.
# The SPA's Sidebar.tsx renders GET /sso-admin/myMenu
# (after commits 0f5b03d + da92221 + 62a0470 + 22aa6ef +
# d7e7412 landed). The backend's MyMenuService.forCaller()
# filters routes per caller via
# RouteRepository.findVisibleForRoles(roleIds). For the
# menu to render anything, the seed data in
# postgres/init/08-seed-sso-admin-routes.sh MUST have run.
#
# 4 checks:
#   1. /myMenu returns 200 for a logged-in admin.
#   2. /myMenu returns >= 12 routes (the twelve the SPA
#      expects to see, per the seed script).
#   3. Each of the twelve expected paths appears in the
#      response (catches a deleted seed row).
#   4. The Spanish legacy labels appear (some were used
#      by the fallback name-keyword icon map in
#      Sidebar.tsx resolveIcon — confirms the rendered
#      icons will resolve before any admin populates the
#      icon column manually).
#
# Negative path (NOT covered here): a non-admin user
# with role bindings sees only the bound subset. That's a
# separate smoke once we can provision a non-admin test
# user in CI without coupling to djromer's password.
#
# Run from project root (assumes the stack is up):
#   bash admin-ui/scripts/smoke-myMenu.sh
# =============================================================================
set -euo pipefail

GATEWAY="${GATEWAY:-http://localhost:8080}"
ADMIN_USER="${SSO_ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${SSO_ADMIN_PASSWORD:-ChangeMe-Now-Please-123!}"

PASS=0
FAIL=0
ok()  { echo "    ✅ $*"; PASS=$((PASS+1)); }
bad() { echo "    ❌ $*"; FAIL=$((FAIL+1)); }
info(){ echo "    $*"; }
hr()  { echo "------------------------------------------------------------"; }

# ---------- login ----------
hr
echo ">>> Login (admin)"

LOGIN_HTTP=$(curl -sS -o /tmp/mymenu-login-body.$$ -w '%{http_code}' \
  -X POST "$GATEWAY/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASSWORD\"}")
if [ "$LOGIN_HTTP" = "200" ]; then
  ok "Login returns 200"
else
  bad "Login expected 200, got $LOGIN_HTTP"
  cat /tmp/mymenu-login-body.$$ || true
  exit 1
fi

ACCESS_JWT=$(python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" \
  < /tmp/mymenu-login-body.$$)
rm -f /tmp/mymenu-login-body.$$

# ---------- 1. /myMenu returns 200 ----------
hr
echo ">>> 1. GET /sso-admin/myMenu returns 200"

GET_HTTP=$(curl -sS -o /tmp/mymenu-body.$$ -w '%{http_code}' \
  -H "Authorization: Bearer $ACCESS_JWT" \
  "$GATEWAY/sso-admin/myMenu")
if [ "$GET_HTTP" = "200" ]; then
  ok "/myMenu returns 200"
else
  bad "/myMenu expected 200, got $GET_HTTP"
  cat /tmp/mymenu-body.$$ || true
  exit 1
fi

# ---------- 2. /myMenu returns >= 12 routes ----------
hr
echo ">>> 2. /myMenu body has >= 12 entries"

ROUTE_COUNT=$(python3 -c "import json; print(len(json.load(open('/tmp/mymenu-body.$$'))))")
if [ "$ROUTE_COUNT" -ge 12 ]; then
  ok "Body has $ROUTE_COUNT routes (>= 12)"
else
  bad "Expected >= 12 routes, got $ROUTE_COUNT"
  cat /tmp/mymenu-body.$$ || true
fi

# ---------- 3. each of the 12 expected paths is present ----------
hr
echo ">>> 3. All twelve expected routes present in /myMenu"

EXPECTED_PATHS=(
  "/admin/users"
  "/admin/roles"
  "/admin/groups"
  "/admin/microservices"
  "/admin/query-services"
  "/admin/queries"
  "/admin/query-catalog"
  "/admin/dynamic-crud"
  "/admin/endpoints"
  "/admin/routes"
  "/admin/apps"
  "/admin/writes"
)
BODY=$(cat /tmp/mymenu-body.$$)
for path in "${EXPECTED_PATHS[@]}"; do
  if echo "$BODY" | grep -q "\"$path\""; then
    ok "$path is in the response"
  else
    bad "$path is MISSING from /myMenu response"
  fi
done

# ---------- 4. the legacy Spanish labels render ----------
hr
echo ">>> 4. Each label is in Spanish (matches legacy sidebar copy)"

EXPECTED_LABELS=(
  "Usuarios"
  "Roles"
  "Grupos"
  "Microservicios"
  "Query Services"
  "Queries Catalog"
  "Queries (admin)"
  "Dynamic CRUD"
  "Endpoints"
  "Rutas"
  "Apps"
  "Writes"
)
for label in "${EXPECTED_LABELS[@]}"; do
  if echo "$BODY" | grep -q "\"$label\""; then
    ok "label '$label' is in the response"
  else
    bad "label '$label' is MISSING from /myMenu response"
  fi
done

rm -f /tmp/mymenu-body.$$

# ---------- summary ----------
hr
TOTAL=$((PASS + FAIL))
echo "    $PASS / $TOTAL checks passed"
[ "$FAIL" -eq 0 ] || exit 1
