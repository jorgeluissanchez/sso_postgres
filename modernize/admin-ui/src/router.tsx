import { createBrowserRouter, Navigate } from "react-router-dom";
import { ActivatePage } from "@/auth/ActivatePage";
import { LoginPage } from "@/auth/LoginPage";
import { RequireAuth } from "@/auth/RequireAuth";
import { App } from "@/App";
import { GroupsListPage } from "@/pages/groups/GroupsListPage";
import { MicroservicesListPage } from "@/pages/microservices/MicroservicesListPage";
import { QueryServicesListPage } from "@/pages/query-services/QueryServicesListPage";
import { QueriesCatalogPage } from "@/pages/queries/QueriesCatalogPage";
import { QueriesAdminPage } from "@/pages/queries/QueriesAdminPage";
import { EndpointsListPage } from "@/pages/endpoints/EndpointsListPage";
import { RoutesListPage } from "@/pages/routes/RoutesListPage";
import { RolesListPage } from "@/pages/roles/RolesListPage";
import { UsersListPage } from "@/pages/users/UsersListPage";

/**
 * Router. Public routes: /login, /activate. Everything under /admin
 * is gated by {@link RequireAuth}. We use the data router so
 * loaders can be added later for prefetch.
 *
 * Note: in production, the api-gateway serves the SPA at /admin/**
 * and falls back to index.html for any unknown path. So the URL
 * stays /admin/users, /admin/roles, etc.
 */
export const router = createBrowserRouter([
  { path: "/login", element: <LoginPage /> },
  { path: "/activate", element: <ActivatePage /> },
  {
    path: "/admin",
    element: <RequireAuth />,
    children: [
      {
        element: <App />,
        children: [
          { index: true, element: <Navigate to="users" replace /> },
          { path: "users", element: <UsersListPage /> },
          { path: "roles", element: <RolesListPage /> },
          { path: "groups", element: <GroupsListPage /> },
          { path: "microservices", element: <MicroservicesListPage /> },
          { path: "query-services", element: <QueryServicesListPage /> },
          { path: "queries", element: <QueriesCatalogPage /> },
          { path: "query-catalog", element: <QueriesAdminPage /> },
          { path: "endpoints", element: <EndpointsListPage /> },
          { path: "routes", element: <RoutesListPage /> },
        ],
      },
    ],
  },
  { path: "/", element: <Navigate to="/admin" replace /> },
  { path: "*", element: <Navigate to="/admin" replace /> },
]);
