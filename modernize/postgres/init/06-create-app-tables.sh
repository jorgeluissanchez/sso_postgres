#!/usr/bin/env bash
# =============================================================================
# 06-create-app-tables.sh
#
# Crea la entidad APP + las 4 tablas M:M (role_app, app_users, app_route,
# app_microservice) + la FK id_app en MICROSERVICE. Refleja el modelo
# SSO_V2 que la modernization no tenía.
#
# Modelo:
#   app             — raíz. id_app, name unique, description, created_date.
#   role_app        — M:N entre APP y ROLE (acceso amplio: un rol ve TODAS
#                     las rutas de la app). Convive con role_route (fino).
#   app_users       — M:N entre APP y users (quienes usan la app).
#   app_route       — M:N entre APP y ROUTE. Es la ÚNICA relación de
#                     membership entre una ruta y sus apps — ROUTE NO
#                     tiene una FK id_app propia (se probó, y se
#                     descartó: un FK singular no puede reflejar que
#                     una ruta pertenezca a más de una app, y además
#                     quedaba desincronizado del M:N cuando un admin
#                     bindeaba/desvinculaba rutas solo por acá).
#   app_microservice— M:N entre APP y MICROSERVICE. Igual rationale que
#                     app_route, pero MICROSERVICE SÍ conserva su FK
#                     id_app como "app primaria" (fuera del alcance de
#                     este cambio; solo ROUTE perdió la suya).
#
# FK directa:
#   MICROSERVICE.id_app    — la app "principal" del microservicio.
#   (ROUTE ya no tiene columna id_app — ver app_route arriba.)
#
# Notas de idempotencia:
#   - CREATE TABLE IF NOT EXISTS para tablas nuevas.
#   - ADD COLUMN IF NOT EXISTS para columnas nuevas sobre tablas que ya
#     existen (ROUTE y MICROSERVICE tienen filas en la modernization).
#   - CREATE INDEX IF NOT EXISTS para todos los índices.
#   - Sin TRUNCATE / DROP — re-ejecutable en cualquier momento.
#
# SKIP screens / reports: el legacy SSO_V2 tenía APP_SCREEN, ROLE_SCREEN,
# REPORT, REPORT_FILTER, ROLE_REPORT. Esos conceptos NO existen en la
# modernization actual (no hay entidad SCREEN ni tabla REPORT); saltarlos
# evita ruido en el schema.
#
# IMPORTANTE: Hibernate está en `ddl-auto=validate` en sso-admin. Cada
# columna que agregue una @Entity debe existir ANTES de que arranque el
# contenedor sso-admin nuevo. El script se ejecuta:
#   1. Antes de reconstruir la imagen sso-admin (psql al contenedor en
#      ejecución, vía el patrón de los scripts 04-/05-).
#   2. Y/o dentro del contenedor postgres en el siguiente cold-start
#      (docker-entrypoint-initdb.d corre todos los 0X- en orden alfabético).
# =============================================================================

set -e

psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER}" --dbname "${POSTGRES_DB}" <<-'EOSQL'

-- APP — agrupación lógica raíz. Una app agrupa rutas + microservicios +
-- usuarios; los roles se bindean a la app para acceso amplio, o a una
-- ruta/microservicio individual para acceso fino (lo que ya existía).
--
-- Convencion: lowercase snake_case (mismo estilo que groups/user_group
-- en 02- y role_app_users en 03-). Hibernate valida los nombres de
-- columna exactos.
CREATE TABLE IF NOT EXISTS app (
    id_app        BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(255) NOT NULL UNIQUE,
    description   VARCHAR(500),
    created_date  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_app_name ON app(name);

-- ROLE_APP — M:N entre app y role. Si un rol está acá, ve TODAS las
-- rutas de esa app (acceso amplio). Convive con role_route, que
-- permite acceso fino a rutas individuales fuera del conjunto.
-- Columnas lowercase: Postgres las crea como `id_app` / `id_role`
-- desde identificadores sin comillas, que es exactamente lo que la
-- entidad va a declarar en @JoinTable.
CREATE TABLE IF NOT EXISTS role_app (
    id_app   BIGINT NOT NULL REFERENCES app(id_app)   ON DELETE CASCADE,
    id_role  BIGINT NOT NULL REFERENCES role(id_role) ON DELETE CASCADE,
    PRIMARY KEY (id_app, id_role)
);

CREATE INDEX IF NOT EXISTS idx_role_app_role ON role_app(id_role);

-- APP_USERS — M:N entre app y users. Lleva registro de qué usuarios
-- pueden acceder a la app (más allá de los permisos por rol). Sirve
-- para membership explícito en la UI de admin.
CREATE TABLE IF NOT EXISTS app_users (
    id_app   BIGINT NOT NULL REFERENCES app(id_app)    ON DELETE CASCADE,
    id_user  BIGINT NOT NULL REFERENCES users(id_user) ON DELETE CASCADE,
    PRIMARY KEY (id_app, id_user)
);

CREATE INDEX IF NOT EXISTS idx_app_users_user ON app_users(id_user);

-- APP_ROUTE — M:N entre app y ROUTE. Es la ÚNICA relación de
-- membership entre una ruta y sus app(s) — permite que una misma
-- ruta aparezca en varias apps. ROUTE no tiene una FK id_app propia.
CREATE TABLE IF NOT EXISTS app_route (
    id_app    BIGINT NOT NULL REFERENCES app(id_app)     ON DELETE CASCADE,
    id_route  BIGINT NOT NULL REFERENCES ROUTE(ID_ROUTE) ON DELETE CASCADE,
    PRIMARY KEY (id_app, id_route)
);

CREATE INDEX IF NOT EXISTS idx_app_route_route ON app_route(id_route);

-- APP_MICROSERVICE — M:N entre app y MICROSERVICE. Misma rationale:
-- membership explícito multi-app, independiente de MICROSERVICE.id_app.
CREATE TABLE IF NOT EXISTS app_microservice (
    id_app          BIGINT NOT NULL REFERENCES app(id_app)                ON DELETE CASCADE,
    id_microservice BIGINT NOT NULL REFERENCES MICROSERVICE(ID_MICROSERVICE) ON DELETE CASCADE,
    PRIMARY KEY (id_app, id_microservice)
);

CREATE INDEX IF NOT EXISTS idx_app_microservice_ms ON app_microservice(id_microservice);

-- FK directa: agrega la columna id_app solo a MICROSERVICE.
-- Nullable (no rompe filas pre-existentes) + ON DELETE SET NULL
-- (borrar la app no borra el microservicio; queda "huérfano" en
-- app=NULL, que es el comportamiento esperado).
--
-- ROUTE NO recibe esta columna — app_route (arriba) es su única
-- relación de membership con App.
ALTER TABLE MICROSERVICE  ADD COLUMN IF NOT EXISTS id_app BIGINT REFERENCES app(id_app) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_microservice_app ON MICROSERVICE(id_app);

EOSQL