/**
 * Mirrored DTOs from the backend. Kept in sync by hand — for ~30
 * types the cost of a Java→TS codegen is not worth the toolchain
 * complexity. The runtime validators in src/api/schemas.ts are the
 * real source of truth for shapes the API actually returns.
 */

// ====================== auth-center ======================

export interface LoginRequest {
  username: string;
  password: string;
}

export interface TokenResponse {
  token: string;
  refreshToken: string;
  expiresIn: number; // seconds
}

export interface UserSummary {
  id: number;
  username: string;
  email: string;
  fullName: string;
  enabled: boolean;
  ldap: boolean;
  roles: string[];
}

// ====================== sso-admin / users ======================

export interface UserResponse {
  id: number;
  username: string;
  fullName: string;
  email: string;
  active: boolean;
  enabled: boolean;
  ldap: boolean;
  roleNames: string[];
}

export interface CreateAccountRequest {
  username: string;
  fullName: string;
  email: string;
  password: string;
  roleNames: string[];
}

export interface UpdateAccountRequest {
  id: number;
  fullName?: string;
  email?: string;
  password?: string;
  roleNames?: string[];
}

export interface BindUserRoleRequest {
  userId: number;
  roleId: number;
}

export interface UserRoleChecked {
  userId: number;
  username: string;
  fullName: string;
  checked: boolean;
}

// ====================== sso-admin / roles ======================

export interface RoleResponse {
  id: number;
  name: string;
  description: string;
}

export interface RoleRequest {
  id?: number;
  name: string;
  description: string;
}

// ====================== sso-admin / groups ======================

export interface GroupResponse {
  id: number;
  name: string;
  description: string;
  memberCount: number;
}

export interface GroupRequest {
  name: string;
  description: string;
}

// ====================== sso-admin / microservice ======================

export type MicroserviceKind = "REST" | "QUERY";

export interface MicroserviceResponse {
  id: number;
  serviceId: string;
  description: string;
  requestUri: string;
  targetUriPath: string;
  targetUrlHost: string;
  targetUrlPort: string;
  createdDate: string;

  /* ====================== provisioning (QUERY kind) ====================== */
  /** Always present (the DB column is NOT NULL DEFAULT 'REST'). */
  kind: MicroserviceKind;
  /** Null for REST rows. */
  dialect: string | null;
  jdbcUrl: string | null;
  dbUsername: string | null;
  /** Backend deliberately omits this from the wire shape
   *  ({@code MicroserviceResponse.fromEntity} returns null
   *  for {@code dbPassword}); the form re-sends it on
   *  updates. */
  dbPassword: string | null;
  poolSize: number | null;
  instanceName: string | null;
}

export interface MicroserviceRequest {
  id?: number;
  serviceId: string;
  description: string;
  requestUri: string;
  targetUriPath: string;
  targetUrlHost: string;
  targetUrlPort: string;

  /* ====================== provisioning (QUERY kind) ====================== */
  /** Defaults to {@code "REST"} on the wire if omitted. */
  kind?: MicroserviceKind;
  dialect?: string | null;
  jdbcUrl?: string | null;
  dbUsername?: string | null;
  dbPassword?: string | null;
  poolSize?: number | null;
  instanceName?: string | null;
}

/* ====================== sso-admin / container status ======================
 *
 * Returned by GET /sso-admin/microservice/{id}/container/status.
 * The backend proxies to the provisioner's
 * GET /provision/{fullName}/status, translates the Docker
 * state string into a normalized value, and adds the
 * timestamp. `state` is null when the container does not
 * exist on the host (provisioner returns 404 → backend
 * returns 200 with state=null so the UI can render an
 * "absent" badge without throwing).
 */
export interface ContainerStatusResponse {
  /** Normalized container state — see StatusBadge for the
   *  full list. Raw Docker state is in {@link rawState}. */
  state:
    | "running"
    | "exited"
    | "created"
    | "paused"
    | "restarting"
    | "dead"
    | "removing"
    | "absent"
    | "provisioning"
    | "unknown";
  /** Original Docker Engine API state string. Useful for
   *  debugging — surfaced in a tooltip on the status badge. */
  rawState: string | null;
  /** Container id from Docker, null when absent. */
  containerId: string | null;
  /** ISO-8601 timestamp of the last container start (per
   *  the Docker inspect response). Null when absent. */
  startedAt: string | null;
  /** Full container name (`query-service-<instanceName>`). */
  fullName: string;
}

// ====================== sso-admin / endpoint ======================

export interface EndpointResponse {
  id: number;
  method: string;
  path: string;
  description: string;
  numberParams: number;
  microserviceIds: number[];
}

export interface EndpointRequest {
  id?: number;
  method: string;
  path: string;
  description: string;
  numberParams: number;
}

export interface EndpointRoleChecked {
  roleId: number;
  name: string;
  checked: boolean;
}

export interface EndpointMicroserviceChecked {
  microserviceId: number;
  serviceId: string;
  checked: boolean;
}

// ====================== sso-admin / route ======================

export interface RouteResponse {
  id: number;
  name: string;
  icon: string;
  path: string;
  menuOrder: number;
  type: string;
  idParent: number | null;
  roleIds: number[];
}

export interface RouteRequest {
  id?: number;
  name: string;
  icon: string;
  path: string;
  menuOrder: number;
  type: string;
  idParent: number | null;
}

export interface RouteRoleChecked {
  roleId: number;
  name: string;
  checked: boolean;
}

// ====================== error envelope ======================

export interface ErrorResponse {
  code: string;
  message: string;
  timestamp: string;
}
