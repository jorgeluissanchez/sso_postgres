/**
 * Typed wrappers over {@link apiClient} — one per backend endpoint.
 * The functions are intentionally thin; no transformation, no
 * caching (TanStack Query handles that). The benefit over calling
 * {@code apiClient.get} directly is a single import for the page
 * code, type safety, and a place to attach Zod runtime validation
 * in a follow-up if a backend field changes shape.
 */
import { apiClient } from "./client";
import type {
  BindUserRoleRequest,
  ContainerStatusResponse,
  CreateAccountRequest,
  EndpointMicroserviceChecked,
  EndpointRequest,
  EndpointResponse,
  EndpointRoleChecked,
  GroupRequest,
  GroupResponse,
  LoginRequest,
  MicroserviceRequest,
  MicroserviceResponse,
  RoleRequest,
  RoleResponse,
  RouteRequest,
  RouteResponse,
  RouteRoleChecked,
  TokenResponse,
  UpdateAccountRequest,
  UserResponse,
  UserRoleChecked,
} from "./types";

// ====================== auth ======================

export const authApi = {
  // skipAuth: a 401 here means "bad credentials", not "expired
  // session" — we don't want the client to try /auth/refresh.
  login: (body: LoginRequest) =>
    apiClient.post<TokenResponse>("/auth/login", body, { skipAuth: true }),
  logout: () => apiClient.post<void>("/auth/logout"),
};

// ====================== users ======================

export const usersApi = {
  list: () => apiClient.get<UserResponse[]>("/sso-admin/getUsers"),
  getRolesByUsername: (username: string) =>
    apiClient.get<string[]>(`/sso-admin/getRolesByUsername?username=${encodeURIComponent(username)}`),
  getRolesByUserId: (userId: number) =>
    apiClient.get<RoleResponse[]>(`/sso-admin/user/roles?userId=${userId}`),
  getRolesChecked: (userId: number) =>
    apiClient.get<UserRoleChecked[]>(`/sso-admin/user/roles/checked?userId=${userId}`),
  create: (body: CreateAccountRequest) => apiClient.post<UserResponse>("/sso-admin/createAccount", body),
  update: (body: UpdateAccountRequest) => apiClient.put<UserResponse>("/sso-admin/updateAccount", body),
  bindRole: (body: BindUserRoleRequest) => apiClient.post<void>("/sso-admin/bindUserRole", body),
  unbindRole: (userId: number, roleId: number) =>
    apiClient.delete<void>(
      `/sso-admin/unbindUserRole?userId=${userId}&roleId=${roleId}`,
    ),
};

// ====================== roles ======================

export const rolesApi = {
  list: () => apiClient.get<RoleResponse[]>("/sso-admin/role/getRoles"),
  create: (body: RoleRequest) => apiClient.post<RoleResponse>("/sso-admin/role/createRole", body),
  update: (body: RoleRequest) => apiClient.put<RoleResponse>("/sso-admin/role/updateRole", body),
  getUsersChecked: (roleId: number) =>
    apiClient.get<UserRoleChecked[]>(`/sso-admin/role/users/checked?roleId=${roleId}`),
};

// ====================== groups ======================

export const groupsApi = {
  list: () => apiClient.get<GroupResponse[]>("/sso-admin/group"),
  save: (body: GroupRequest) => apiClient.post<GroupResponse>("/sso-admin/group", body),
  bindUser: (userId: number, groupId: number) =>
    apiClient.post<void>("/sso-admin/group/bindUserGroup", { userId, groupId }),
};

// ====================== microservice ======================

export const microservicesApi = {
  list: () => apiClient.get<MicroserviceResponse[]>("/sso-admin/microservice/getMicroservices"),
  create: (body: MicroserviceRequest) =>
    apiClient.post<MicroserviceResponse>("/sso-admin/microservice/save", body),
  update: (body: MicroserviceRequest) =>
    apiClient.put<MicroserviceResponse>("/sso-admin/microservice/update", body),
  delete: (id: number) => apiClient.delete<void>(`/sso-admin/microservice/${id}`),
};

/* ====================== query-service container (provisioner-backed) ======================
 *
 * The provisioner is internal-only (compose port 9000 has no
 * host mapping). sso-admin proxies these calls so the browser
 * only ever talks to the gateway. The endpoints here describe
 * the contract the backend is expected to implement; the admin-ui
 * ships them now so the page is ready when the proxy endpoints
 * land.
 *
 * POST /restart returns 202 Accepted — the restart is async
 * (provisioner issues stop+start, takes 1-3s). The UI fires it
 * and the next status poll picks up the new state.
 */
export const queryServicesApi = {
  status: (id: number) =>
    apiClient.get<ContainerStatusResponse>(`/sso-admin/microservice/${id}/container/status`),
  logs: (id: number, tail = 200) =>
    apiClient.get<string>(`/sso-admin/microservice/${id}/container/logs?tail=${tail}`),
  restart: (id: number) =>
    apiClient.post<void>(`/sso-admin/microservice/${id}/container/restart`),
};

// ====================== endpoint ======================

export const endpointsApi = {
  list: () => apiClient.get<EndpointResponse[]>("/sso-admin/endpoint/getEndpoints"),
  create: (body: EndpointRequest) =>
    apiClient.post<EndpointResponse>("/sso-admin/endpoint/save", body),
  update: (body: EndpointRequest) =>
    apiClient.put<EndpointResponse>("/sso-admin/endpoint/update", body),
  delete: (id: number) => apiClient.delete<void>(`/sso-admin/endpoint/${id}`),
  bindMicroservice: (id: number, microserviceId: number) =>
    apiClient.post<void>(`/sso-admin/endpoint/${id}/microservice/${microserviceId}`),
  unbindMicroservice: (id: number, microserviceId: number) =>
    apiClient.delete<void>(`/sso-admin/endpoint/${id}/microservice/${microserviceId}`),
  getMicroservicesChecked: (id: number) =>
    apiClient.get<EndpointMicroserviceChecked[]>(`/sso-admin/endpoint/${id}/microservices/checked`),
  bindRole: (id: number, roleId: number) =>
    apiClient.post<void>(`/sso-admin/endpoint/${id}/role/${roleId}`),
  unbindRole: (id: number, roleId: number) =>
    apiClient.delete<void>(`/sso-admin/endpoint/${id}/role/${roleId}`),
  getRolesChecked: (id: number) =>
    apiClient.get<EndpointRoleChecked[]>(`/sso-admin/endpoint/${id}/roles/checked`),
};

// ====================== route ======================

export const routesApi = {
  list: () => apiClient.get<RouteResponse[]>("/sso-admin/route/getRoutes"),
  listByParent: (idParent: number | null) => {
    const q = idParent == null ? "" : `?idParent=${idParent}`;
    return apiClient.get<RouteResponse[]>(`/sso-admin/route/getRoutesByParent${q}`);
  },
  create: (body: RouteRequest) => apiClient.post<RouteResponse>("/sso-admin/route/save", body),
  update: (body: RouteRequest) => apiClient.put<RouteResponse>("/sso-admin/route/update", body),
  delete: (id: number) => apiClient.delete<void>(`/sso-admin/route/${id}`),
  bindRole: (id: number, roleId: number) =>
    apiClient.post<void>(`/sso-admin/route/${id}/role/${roleId}`),
  unbindRole: (id: number, roleId: number) =>
    apiClient.delete<void>(`/sso-admin/route/${id}/role/${roleId}`),
  getRolesChecked: (id: number) =>
    apiClient.get<RouteRoleChecked[]>(`/sso-admin/route/${id}/roles/checked`),
};
