import { NavLink } from "react-router-dom";

const items = [
  { to: "/admin/users", label: "Usuarios" },
  { to: "/admin/roles", label: "Roles" },
  { to: "/admin/groups", label: "Grupos" },
  { to: "/admin/microservices", label: "Microservicios" },
  { to: "/admin/queries", label: "Queries" },
  { to: "/admin/query-catalog", label: "Queries admin" },
  { to: "/admin/endpoints", label: "Endpoints" },
  { to: "/admin/routes", label: "Rutas" },
  { to: "/admin/apps", label: "Apps" },
  { to: "/admin/writes", label: "Writes" },
];

export function Sidebar() {
  return (
    <nav aria-label="Navegación principal" className="w-56 shrink-0 border-r border-slate-200 bg-white">
      <ul className="p-2">
        {items.map((item) => (
          <li key={item.to}>
            <NavLink
              to={item.to}
              className={({ isActive }) =>
                [
                  "block rounded px-3 py-2 text-sm",
                  isActive
                    ? "bg-sky-50 font-medium text-sky-700"
                    : "text-slate-700 hover:bg-slate-50",
                ].join(" ")
              }
            >
              {item.label}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}
