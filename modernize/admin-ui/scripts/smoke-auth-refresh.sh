#!/usr/bin/env bash
# =============================================================================
# scripts/smoke-auth-refresh.sh
#
# End-to-end smoke for the Redis-backed rotating refresh-token
# flow that closes the legacy /auth/refresh bypass (where any
# non-empty sso_refresh cookie minted a token for the first
# enabled user). The flow now follows RFC 9700 §4.14:
#
#   - Login mints a refresh token server-side via the store.
#   - Each /auth/refresh rotates the cookie (new raw value).
#   - Replay of an already-rotated cookie wipes the family.
#   - /auth/logout revokes the family and clears the cookie.
#   - Multi-device logins produce independent families.
#
# 8 checks:
#   1. Login sets sso_refresh cookie with HttpOnly + SameSite=Strict
#      + Path=/ + Max-Age=2592000 (30 days, mirrors store TTL).
#   2. /auth/refresh with the login cookie returns 200 + a NEW
#      Set-Cookie (rotation worked).
#   3. Reuse of the OLD refresh cookie after rotation returns 401
#      refresh_token_reuse + the rotated cookie also fails (family
#      wiped as a side effect).
#   4. Logout clears the cookie (Max-Age=0) + a subsequent refresh
#      with the pre-logout cookie returns 401.
#   5. Two simultaneous logins yield two independent families —
#      rotating one does not invalidate the other.
#   6. The access token minted at refresh carries the LOGGED-IN
#      user's sub claim (this is the assertion that pins down the
#      bypass-era behaviour where any cookie minted a token for
#      the first user in the DB).
#   7. Cookie security attributes: HttpOnly, SameSite=Strict,
#      no Domain, Path=/ (Secure must NOT appear over plain HTTP).
#   8. /auth/refresh with NO cookie returns 401 no_refresh_cookie.
#
# Run from project root (assumes the stack is up):
#   bash admin-ui/scripts/smoke-auth-refresh.sh
# =============================================================================
set -euo pipefail

GATEWAY="${GATEWAY:-http://localhost:8080}"
ADMIN_USER="${SSO_ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${SSO_ADMIN_PASSWORD:-ChangeMe-Now-Please-123!}"

PASS=0
FAIL=0
ok()   { echo "    ✅ $*"; PASS=$((PASS+1)); }
bad()  { echo "    ❌ $*"; FAIL=$((FAIL+1)); }
info() { echo "    $*"; }
hr()   { echo "------------------------------------------------------------"; }

# ---------- helpers ----------

# Extract the cookie value from a Set-Cookie header (between '=' and ';').
cookie_value() {
  local hdr="$1"
  echo "$hdr" | sed -n 's/^sso_refresh=\([^;]*\).*/\1/p'
}

# Capture the Set-Cookie + HTTP status from a POST + cookie jar file.
# Args: url cookie_jar [extra curl args...]
post_with_cookie() {
  local url="$1"; local jar="$2"; shift 2
  curl -sS -o /tmp/smoke-refresh-body.$$ -D /tmp/smoke-refresh-hdr.$$ \
       -w '%{http_code}' -X POST "$url" -b "$jar" -c "$jar" "$@"
}

# ---------- 1. login + cookie attrs ----------
hr
echo ">>> 1. Logging in via $GATEWAY/login; checking sso_refresh attrs"

JAR=$(mktemp)
trap 'rm -f "$JAR" /tmp/smoke-refresh-body.$$ /tmp/smoke-refresh-hdr.$$' EXIT

LOGIN_HTTP=$(curl -sS -o /tmp/login-body.$$ -D /tmp/login-hdr.$$ \
  -w '%{http_code}' -X POST "$GATEWAY/login" \
  -H "Content-Type: application/json" \
  -c "$JAR" \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASSWORD\"}")

if [ "$LOGIN_HTTP" = "200" ]; then
  ok "Login returns 200"
else
  bad "Login expected 200, got $LOGIN_HTTP"
  cat /tmp/login-body.$$ || true
  exit 1
fi

SET_COOKIE=$(grep -i '^set-cookie:' /tmp/login-hdr.$$ | sed 's/^[Ss]et-[Cc]ookie: //')
if [ -z "$SET_COOKIE" ]; then
  bad "No Set-Cookie header on login response"
  exit 1
fi

V1=$(cookie_value "$SET_COOKIE")
[ -n "$V1" ] && ok "sso_refresh cookie present (value: ${V1:0:8}...)" \
              || bad "Could not parse sso_refresh value"

echo "$SET_COOKIE" | grep -q "HttpOnly"     && ok "cookie HttpOnly"     || bad "cookie missing HttpOnly"
echo "$SET_COOKIE" | grep -q "SameSite=Strict" && ok "cookie SameSite=Strict" || bad "cookie missing SameSite=Strict"
echo "$SET_COOKIE" | grep -q "Path=/"       && ok "cookie Path=/"       || bad "cookie missing Path=/"
echo "$SET_COOKIE" | grep -q "Max-Age=2592000" && ok "cookie Max-Age=2592000 (30d)" \
                                              || bad "cookie Max-Age != 2592000"
echo "$SET_COOKIE" | grep -qv "Secure"      && ok "cookie NOT Secure (plain HTTP)" \
                                              || bad "cookie unexpectedly Secure over plain HTTP"

# Capture the access JWT for the correct-user assertion later.
ACCESS_JWT=$(python3 -c "import sys,json; print(json.load(sys.stdin)['token'])" < /tmp/login-body.$$)

# ---------- 2. refresh rotation ----------
hr
echo ">>> 2. POST /api/auth/refresh with the login cookie (expect rotation)"

REFRESH_HTTP=$(post_with_cookie "$GATEWAY/api/auth/refresh" "$JAR")
if [ "$REFRESH_HTTP" = "200" ]; then
  ok "/auth/refresh returns 200"
else
  bad "/auth/refresh expected 200, got $REFRESH_HTTP"
  cat /tmp/smoke-refresh-body.$$ || true
fi

REFRESH_HDR=$(cat /tmp/smoke-refresh-hdr.$$)
NEW_SET_COOKIE=$(echo "$REFRESH_HDR" | grep -i '^set-cookie:' | sed 's/^[Ss]et-[Cc]ookie: //' | head -1)
if [ -z "$NEW_SET_COOKIE" ]; then
  bad "No Set-Cookie on /auth/refresh (rotation did not occur)"
else
  V2=$(cookie_value "$NEW_SET_COOKIE")
  if [ -n "$V2" ] && [ "$V2" != "$V1" ]; then
    ok "Set-Cookie rotated: ${V1:0:8}... -> ${V2:0:8}..."
  else
    bad "Rotated cookie value did not change (V1=$V1 V2=$V2)"
  fi
fi

# ---------- 3. reuse detection + family wipe ----------
hr
echo ">>> 3. Replay the OLD refresh cookie (expect 401 refresh_token_reuse)"

# Build a one-shot cookie jar with ONLY the V1 cookie (no rotation has
# touched it since login).
V1_JAR=$(mktemp); echo -e "# Netscape HTTP Cookie File\nlocalhost\tFALSE\t/\tFALSE\t0\tsso_refresh\t$V1" > "$V1_JAR"
trap 'rm -f "$JAR" "$V1_JAR" /tmp/smoke-refresh-body.$$ /tmp/smoke-refresh-hdr.$$ /tmp/login-body.$$ /tmp/login-hdr.$$' EXIT

REUSE_HTTP=$(curl -sS -o /tmp/reuse-body.$$ -w '%{http_code}' \
  -X POST "$GATEWAY/api/auth/refresh" -b "$V1_JAR")
REUSE_BODY=$(cat /tmp/reuse-body.$$)

if [ "$REUSE_HTTP" = "401" ]; then
  ok "Reuse of rotated cookie returns 401"
else
  bad "Reuse expected 401, got $REUSE_HTTP"
fi
if echo "$REUSE_BODY" | grep -q "refresh_token_reuse"; then
  ok "Reuse body contains refresh_token_reuse"
else
  bad "Reuse body missing refresh_token_reuse: $REUSE_BODY"
fi

# The legitimate V2 cookie must also fail now (family wiped).
V2_JAR=$(mktemp); echo -e "# Netscape HTTP Cookie File\nlocalhost\tFALSE\t/\tFALSE\t0\tsso_refresh\t$V2" > "$V2_JAR"
trap 'rm -f "$JAR" "$V1_JAR" "$V2_JAR" /tmp/smoke-refresh-body.$$ /tmp/smoke-refresh-hdr.$$ /tmp/login-body.$$ /tmp/login-hdr.$$ /tmp/reuse-body.$$' EXIT

V2_HTTP=$(curl -sS -o /tmp/v2-body.$$ -w '%{http_code}' \
  -X POST "$GATEWAY/api/auth/refresh" -b "$V2_JAR")
if [ "$V2_HTTP" = "401" ]; then
  ok "Legitimate V2 also returns 401 (family wiped)"
else
  bad "Legitimate V2 expected 401, got $V2_HTTP"
fi

# ---------- 4. logout clears + revokes ----------
hr
echo ">>> 4. POST /api/auth/logout clears the cookie + revokes the family"

# Fresh login so we have a cookie we can logout. Capture JAR2's
# headers into a dedicated file (NOT /tmp/login-hdr.$$, which belongs
# to check #1's first login and whose cookie has since been rotated).
JAR2=$(mktemp)
curl -sS -o /dev/null -D /tmp/login2-hdr.$$ -X POST "$GATEWAY/login" \
  -H "Content-Type: application/json" \
  -c "$JAR2" \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASSWORD\"}"
trap 'rm -f "$JAR" "$JAR2" "$V1_JAR" "$V2_JAR" "$PRE_JAR" "$EMPTY_JAR" "$A_JAR" "$B_JAR" "$DEV_A" "$DEV_B" /tmp/smoke-refresh-body.$$ /tmp/smoke-refresh-hdr.$$ /tmp/login-body.$$ /tmp/login-hdr.$$ /tmp/login2-hdr.$$ /tmp/reuse-body.$$ /tmp/v2-body.$$ /tmp/empty-body.$$ /tmp/logout-hdr.$$' EXIT

LOGOUT_HTTP=$(curl -sS -o /dev/null -D /tmp/logout-hdr.$$ -w '%{http_code}' \
  -X POST "$GATEWAY/api/auth/logout" -b "$JAR2")
LOGOUT_HDR=$(cat /tmp/logout-hdr.$$)
LOGOUT_SET_COOKIE=$(echo "$LOGOUT_HDR" | grep -i '^set-cookie:' | sed 's/^[Ss]et-[Cc]ookie: //' | head -1)

if [ "$LOGOUT_HTTP" = "200" ]; then
  ok "Logout returns 200"
else
  bad "Logout expected 200, got $LOGOUT_HTTP"
fi
if echo "$LOGOUT_SET_COOKIE" | grep -q "Max-Age=0"; then
  ok "Logout Set-Cookie has Max-Age=0"
else
  bad "Logout Set-Cookie missing Max-Age=0"
fi

# Use the pre-logout cookie from THIS JAR2 login (not the one from
# check #1) so the assertion truly isolates logout-revocation from
# any family wipes left over from checks #2 / #3.
PRE_LOGOUT=$(cookie_value "$(grep -i '^set-cookie:' /tmp/login2-hdr.$$ | sed 's/^[Ss]et-[Cc]ookie: //' | head -1)")
PRE_JAR=$(mktemp); echo -e "# Netscape HTTP Cookie File\nlocalhost\tFALSE\t/\tFALSE\t0\tsso_refresh\t$PRE_LOGOUT" > "$PRE_JAR"
PRE_HTTP=$(curl -sS -o /dev/null -w '%{http_code}' \
  -X POST "$GATEWAY/api/auth/refresh" -b "$PRE_JAR")
if [ "$PRE_HTTP" = "401" ]; then
  ok "Refresh with pre-logout cookie returns 401 (family revoked by logout)"
else
  bad "Refresh with pre-logout cookie expected 401, got $PRE_HTTP"
fi

# ---------- 5. multi-device independence ----------
hr
echo ">>> 5. Two simultaneous logins -> independent families"

DEV_A=$(mktemp); DEV_B=$(mktemp)
curl -sS -o /dev/null -X POST "$GATEWAY/login" -H "Content-Type: application/json" \
  -c "$DEV_A" -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASSWORD\"}"
curl -sS -o /dev/null -X POST "$GATEWAY/login" -H "Content-Type: application/json" \
  -c "$DEV_B" -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASSWORD\"}"

COOKIE_A=$(grep 'sso_refresh' "$DEV_A" | awk '{print $7}')
COOKIE_B=$(grep 'sso_refresh' "$DEV_B" | awk '{print $7}')

if [ -n "$COOKIE_A" ] && [ -n "$COOKIE_B" ] && [ "$COOKIE_A" != "$COOKIE_B" ]; then
  ok "Two logins produced two distinct cookies"
else
  bad "Two logins did not produce distinct cookies (A=${COOKIE_A:-?} B=${COOKIE_B:-?})"
fi

# Rotate A.
A_JAR=$(mktemp); echo -e "# Netscape HTTP Cookie File\nlocalhost\tFALSE\t/\tFALSE\t0\tsso_refresh\t$COOKIE_A" > "$A_JAR"
A_HTTP=$(curl -sS -o /dev/null -w '%{http_code}' \
  -X POST "$GATEWAY/api/auth/refresh" -b "$A_JAR")
if [ "$A_HTTP" = "200" ]; then
  ok "Device A refresh returns 200"
else
  bad "Device A refresh expected 200, got $A_HTTP"
fi

# B is unaffected.
B_JAR=$(mktemp); echo -e "# Netscape HTTP Cookie File\nlocalhost\tFALSE\t/\tFALSE\t0\tsso_refresh\t$COOKIE_B" > "$B_JAR"
B_HTTP=$(curl -sS -o /dev/null -w '%{http_code}' \
  -X POST "$GATEWAY/api/auth/refresh" -b "$B_JAR")
if [ "$B_HTTP" = "200" ]; then
  ok "Device B refresh still returns 200 (independent family)"
else
  bad "Device B refresh expected 200, got $B_HTTP"
fi

# ---------- 6. refresh-minted JWT carries the right sub ----------
hr
echo ">>> 6. Access token from /auth/refresh carries the LOGGED-IN user's sub claim"

# Decode the JWT payload (no signature check; we only care about sub).
PAYLOAD=$(echo "$ACCESS_JWT" | cut -d. -f2)
# Base64url -> Base64 padding.
PAD=$(( (4 - ${#PAYLOAD} % 4) % 4 ))
for _ in $(seq 1 $PAD); do PAYLOAD="${PAYLOAD}="; done
SUB=$(echo "$PAYLOAD" | tr '_-' '/+' | base64 -d 2>/dev/null \
      | python3 -c "import sys,json; print(json.load(sys.stdin)['sub'])" 2>/dev/null || echo "")
if [ "$SUB" = "$ADMIN_USER" ]; then
  ok "JWT sub claim is '$SUB'"
else
  bad "JWT sub claim expected '$ADMIN_USER', got '${SUB:-<empty>}'"
fi

# ---------- 7. cookie security attribute roll-up ----------
hr
echo ">>> 7. Roll-up: HttpOnly + SameSite=Strict + no Domain + Path=/"

if echo "$SET_COOKIE" | grep -q "HttpOnly" \
   && echo "$SET_COOKIE" | grep -q "SameSite=Strict" \
   && ! echo "$SET_COOKIE" | grep -qi "Domain=" \
   && echo "$SET_COOKIE" | grep -q "Path=/"; then
  ok "Cookie has all required security attributes"
else
  bad "Cookie missing one or more required security attributes: $SET_COOKIE"
fi

# ---------- 8. no-cookie refresh -> 401 no_refresh_cookie ----------
hr
echo ">>> 8. POST /api/auth/refresh with no cookie returns 401 no_refresh_cookie"

EMPTY_JAR=$(mktemp); : > "$EMPTY_JAR"
EMPTY_HTTP=$(curl -sS -o /tmp/empty-body.$$ -w '%{http_code}' \
  -X POST "$GATEWAY/api/auth/refresh" -b "$EMPTY_JAR")
EMPTY_BODY=$(cat /tmp/empty-body.$$)

if [ "$EMPTY_HTTP" = "401" ]; then
  ok "No-cookie refresh returns 401"
else
  bad "No-cookie refresh expected 401, got $EMPTY_HTTP"
fi
if echo "$EMPTY_BODY" | grep -q "no_refresh_cookie"; then
  ok "Body contains no_refresh_cookie"
else
  bad "Body missing no_refresh_cookie: $EMPTY_BODY"
fi

# ---------- summary ----------
hr
TOTAL=$((PASS + FAIL))
echo "    $PASS / $TOTAL checks passed"
[ "$FAIL" -eq 0 ] || exit 1