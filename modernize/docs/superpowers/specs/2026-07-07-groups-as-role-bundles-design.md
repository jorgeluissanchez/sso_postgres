# Groups as Role Bundles — Design

**Date:** 2026-07-07
**Status:** Approved (design), pending implementation plan
**Module scope:** `common`, `auth-center`, `sso-admin`, `admin-ui`, `postgres/migrations`

## Problem

In the modernized stack, groups (`groups` / `user_group`) are an orphaned
CRUD: a group is just a named bag of users with no effect on anything.
Access is resolved entirely from the **roles carried in the JWT**
(`MyMenuService`: a role bound to an App via `role_app` grants the app's
routes; a role bound to a route via `role_route` grants that route).
Nothing consults users or groups for access.

The legacy `sso-service` bound groups to apps (`bindAppGroup`). Porting
that literally would add a second, parallel access path (group → app)
alongside the role-driven one — two access models to reason about, and a
change to `MyMenuService` and the gateway's authorization view.

## Decision

Redesign groups as **role bundles** (standard RBAC): a group aggregates
**roles**. A user's effective roles are their direct roles union the roles
of every group they belong to. This makes groups useful, keeps a single
access model (everything still flows through roles), and requires **zero**
changes to `MyMenuService` or the gateway — group roles simply appear in
the JWT like any other role.

`bindAppGroup` (group → app) is **dropped**; it does not fit the
role-driven model.

## Architecture

### Data model

- **New table `group_role`** (M:N), migration `V8__create_group_role.sql`:
  - `group_id BIGINT NOT NULL REFERENCES groups(id_group) ON DELETE CASCADE`
  - `role_id  BIGINT NOT NULL REFERENCES role(id_role)   ON DELETE CASCADE`
  - `PRIMARY KEY (group_id, role_id)`
  - index on `role_id` for the reverse lookup.
- **`Group` entity** (`common`): add
  `@ManyToMany Set<Role> roles` joined through `group_role`
  (`joinColumns = group_id`, `inverseJoinColumns = role_id`). Keep the
  existing `users` (`user_group`) relation unchanged.
- `User` already has `groups` (`user_group`) and `roles` (`role_users`) —
  no schema change.

### Effective roles (the core)

Effective roles of a user = `user.roles` ∪ (⋃ over `user.groups` of
`group.roles`), de-duplicated by role name.

A single helper computes this so the three token-issue sites stay in sync:

- **`EffectiveRoles.of(User)`** (or an injectable resolver in
  `auth-center`) returns the `Set<String>` of role names.
- Applied at the **three points where auth-center builds a JWT**, all of
  which currently read roles directly:
  1. Login — `JsonLoginFilter` (via `User.getAuthorities()`).
  2. Refresh — `RefreshController` (currently `user.getRoles().stream()...`).
  3. API token — `AuthController` (`roleNames(user)`).

**Expansion happens at token-issue time, not per request.** Once the
union is in the JWT, all downstream consumers (`MyMenuService`, gateway
authorization) work unchanged because they already read roles from the
JWT. Trade-off: changing a group's roles takes effect on the user's next
login or token refresh (access tokens are 1h), which is acceptable.

**Lazy-loading note:** `user.groups` and `group.roles` are `LAZY`. The
resolver must load them inside an active persistence context — either a
`@Transactional` boundary at each issue site or a dedicated JPQL
`join fetch` query (`SELECT u FROM User u LEFT JOIN FETCH u.roles LEFT
JOIN FETCH u.groups g LEFT JOIN FETCH g.roles WHERE u.username = :name`).
The implementation plan will pick the fetch-query approach to avoid
relying on open sessions at filter time.

### Admin API (sso-admin `GroupController`)

Mirror the existing App role-binding pattern
(`AppController`: `POST /{id}/role/{roleId}` + `GET /{id}/roles/checked`):

- `POST /group/{id}/role/{roleId}` — toggle bind/unbind of a role to the group.
- `GET  /group/{id}/roles/checked` — roles with a `checked` flag for the group.
- Keep `bindUserGroup` (user↔group). Do **not** add `bindAppGroup`.

`GroupAdminService` gains the role-binding methods and a `RoleChecked`
projection consistent with `AppService.RoleChecked`.

### Admin UI

- Group form drawer gains a **"Roles"** tab, reusing the existing
  `BindingTab` component (same one App's binding tabs use).
- Wire a `useGroupRoles` hook / endpoints in `api/endpoints.ts` mirroring
  the app-role binding hooks.

## Error handling

- Binding a non-existent group or role → `404` via the existing
  `GlobalExceptionHandler` / `NotFoundException`.
- Re-binding an already-bound role → idempotent no-op (toggle semantics),
  not an error.
- Effective-roles resolver on a user with no groups → returns direct
  roles only (empty-group union is inert).

## Testing

- **Unit** — `EffectiveRoles`: direct-only, group-only, and overlapping
  (a role held both directly and via a group appears once).
- **Integration** (`sso-admin`) — create group with role X, bind user to
  group, assert `GET /group/{id}/roles/checked` reflects X.
- **Integration** (`auth-center`) — user whose only path to role X is
  through a group: log in, decode the JWT, assert X is present; hit
  `/myMenu` (through the gateway view or the service) and assert the
  routes X grants via `role_app` are returned.

## Out of scope (YAGNI)

- No `group → app` or `group → route` bindings. Groups bind to roles only.
- No change to the gateway authorization chain (it reads JWT roles).
- No real-time propagation of group changes to live tokens (next
  login/refresh picks them up).
- No group hierarchy / nested groups.
