import { Outlet } from "react-router-dom";
import { Topbar } from "@/layout/Topbar";

/**
 * Layout shell rendered inside <RequireAuth>. A topbar with the
 * logged-in user and a logout button, and a main content area
 * for the routed page.
 */
export function App() {
  return (
    <div className="min-h-screen bg-slate-50">
      <Topbar />
      <main className="mx-auto max-w-7xl px-6 py-6">
        <Outlet />
      </main>
    </div>
  );
}
