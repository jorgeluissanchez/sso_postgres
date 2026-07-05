import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ToastProvider } from "@/components/ui/Toast";
import { WritesListPage } from "./WritesListPage";
import type {
  WriteDefinitionResponse,
  WriteRoleChecked,
} from "@/api/types";

/**
 * Tests for the Writes admin CRUD page. Same fetch-stub +
 * URL-dispatch pattern as {@code AppsListPage.test.tsx}.
 *
 * <p>Coverage scope (mirrors the Apps test set + chips):
 * <ul>
 *   <li>Renders rows from {@code GET /write/getWrites} with the
 *       columns + roles badge.</li>
 *   <li>"+ Nuevo write" opens the drawer; submitting POSTs to
 *       {@code /write/save} with JSON-as-string columns.
 *       Drawer closes.</li>
 *   <li>"Editar" opens the drawer with the row's chip data;
 *       submit PUTs to {@code /write/update}. Drawer stays
 *       open.</li>
 *   <li>Chip add/remove round-trip inside the edit drawer
 *       (no fetch — the form state is what we're after).</li>
 *   <li>Roles tab lists {@code roleId}+{@code name}+{@code checked}
 *       rows and toggles with POST/DELETE on
 *       {@code /write/{id}/role/{roleId}}.</li>
 *   <li>"Eliminar" opens the confirm modal; on confirm,
 *       {@code DELETE /write/{id}} fires.</li>
 *   <li>Empty state shows the configured copy.</li>
 * </ul>
 */

function renderPage() {
  const qc = new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 0,
        gcTime: 0,
        retry: false,
        refetchOnWindowFocus: false,
      },
      mutations: { retry: false },
    },
  });
  return render(
    <QueryClientProvider client={qc}>
      <ToastProvider>
        <MemoryRouter>
          <WritesListPage />
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  );
}

function mkWrite(over: Partial<WriteDefinitionResponse> = {}): WriteDefinitionResponse {
  return {
    id: 1,
    uuid: "smoke-write-1",
    writeType: "INSERT",
    tableName: "public.users",
    columns: '["ID","NAME"]',
    keyColumns: null,
    createdDate: "2026-07-01T00:00:00Z",
    roleIds: [10],
    ...over,
  };
}

function mkRoleChecked(r: Partial<WriteRoleChecked>): WriteRoleChecked {
  return { roleId: 1, name: "ADMIN", checked: false, ...r };
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function findFetchCall(fetchSpy: ReturnType<typeof vi.fn>, urlFragment: string) {
  return fetchSpy.mock.calls.find(([url]) => {
    const u = typeof url === "string" ? url : (url as URL).toString();
    return u.includes(urlFragment);
  });
}

/**
 * Builds a fetch spy keyed by URL fragment. Write endpoints
 * follow the apps-style URL layout:
 * <ul>
 *   <li>GET /sso-admin/write/getWrites — list</li>
 *   <li>POST /sso-admin/write/save — create (no id)</li>
 *   <li>PUT /sso-admin/write/update — update (id in body)</li>
 *   <li>DELETE /sso-admin/write/{id} — delete</li>
 *   <li>POST /sso-admin/write/{id}/role/{roleId} — bind role</li>
 *   <li>DELETE /sso-admin/write/{id}/role/{roleId} — unbind role</li>
 *   <li>GET /sso-admin/write/{id}/roles/checked — roles list</li>
 * </ul>
 */
function buildFetchSpy(opts: {
  writes: WriteDefinitionResponse[];
  rolesForId?: Record<number, WriteRoleChecked[]>;
}) {
  const fetchSpy = vi.fn(
    (url: string | URL | Request, init?: RequestInit) => {
      const u = typeof url === "string" ? url : url.toString();
      const method = (init?.method ?? "GET").toUpperCase();

      // list
      if (method === "GET" && u.includes("/sso-admin/write/getWrites")) {
        return Promise.resolve(jsonResponse(opts.writes));
      }
      // create echo
      if (method === "POST" && u.includes("/sso-admin/write/save")) {
        const body = JSON.parse((init?.body as string) ?? "{}");
        return Promise.resolve(
          jsonResponse({
            id: 999,
            createdDate: "2026-07-01T00:00:00Z",
            roleIds: [],
            ...body,
          }),
        );
      }
      // update echo
      if (method === "PUT" && u.includes("/sso-admin/write/update")) {
        const body = JSON.parse((init?.body as string) ?? "{}");
        return Promise.resolve(jsonResponse(body));
      }
      // delete /write/{id}
      if (method === "DELETE" && /\/sso-admin\/write\/\d+$/.test(u)) {
        return Promise.resolve(new Response(null, { status: 204 }));
      }

      // role toggle + checked
      const roleToggleMatch = u.match(
        /\/sso-admin\/write\/(\d+)\/role\/(\d+)$/,
      );
      if (roleToggleMatch) {
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      const roleCheckedMatch = u.match(
        /\/sso-admin\/write\/(\d+)\/roles\/checked$/,
      );
      if (method === "GET" && roleCheckedMatch) {
        const id = Number(roleCheckedMatch[1]);
        return Promise.resolve(jsonResponse(opts.rolesForId?.[id] ?? []));
      }

      return Promise.reject(new Error(`Unexpected fetch: ${method} ${u}`));
    },
  );
  return fetchSpy;
}

describe("WritesListPage", () => {
  let fetchSpy: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    fetchSpy = vi.fn();
    vi.stubGlobal("fetch", fetchSpy);
  });
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("renders rows from /getWrites with columns + roles badges", async () => {
    const w1 = mkWrite({ id: 1, uuid: "smoke-write-1", roleIds: [10] });
    const w2 = mkWrite({ id: 2, uuid: "smoke-write-2", roleIds: [] });
    const spy = buildFetchSpy({ writes: [w1, w2] });
    vi.stubGlobal("fetch", spy);

    renderPage();
    expect(await screen.findByText("smoke-write-1")).toBeInTheDocument();
    expect(screen.getByText("smoke-write-2")).toBeInTheDocument();
    // writeType badges render
    expect(screen.getAllByText("INSERT").length).toBeGreaterThanOrEqual(1);
    // roles counts — template literal → 2 text nodes, use regex
    expect(screen.getByText(/Roles · 1/)).toBeInTheDocument();
    expect(screen.getByText(/Roles · 0/)).toBeInTheDocument();
    // columns count (2) shows for both
    expect(screen.getAllByText(/^2/).length).toBeGreaterThanOrEqual(1);
    // actions
    expect(screen.getByTestId("edit-1")).toBeInTheDocument();
    expect(screen.getByTestId("delete-1")).toBeInTheDocument();
    expect(screen.getByTestId("new-write")).toBeInTheDocument();
  });

  it("submitting the new-write drawer POSTs to /write/save with JSON columns and closes", async () => {
    const spy = buildFetchSpy({ writes: [] });
    vi.stubGlobal("fetch", spy);

    renderPage();
    await userEvent.click(await screen.findByTestId("new-write"));

    await userEvent.type(screen.getByLabelText(/UUID/i), "smoke-write-new");
    await userEvent.type(screen.getByLabelText(/Tabla/i), "public.users");
    // The select fires onChange properly — leave default INSERT.
    await userEvent.type(
      screen.getByTestId("write-columns-input"),
      "ID{Enter}",
    );

    await userEvent.click(screen.getByRole("button", { name: /^Crear$/i }));

    await waitFor(() =>
      expect(findFetchCall(spy, "/sso-admin/write/save")).toBeDefined(),
    );
    const call = findFetchCall(spy, "/sso-admin/write/save")!;
    expect((call[1] as RequestInit).method).toBe("POST");
    const body = JSON.parse((call[1] as RequestInit).body as string);
    expect(body.uuid).toBe("smoke-write-new");
    expect(body.tableName).toBe("public.users");
    // columns JSON-as-string
    expect(body.columns).toBe('["ID"]');
    expect(body.keyColumns).toBeNull();

    // drawer closes on create
    await waitFor(() =>
      expect(screen.queryByLabelText(/UUID/i)).not.toBeInTheDocument(),
    );
  });

  it("editing submits to /write/update and keeps the drawer open", async () => {
    const w1 = mkWrite({
      id: 1,
      uuid: "smoke-write-1",
      columns: '["ID","NAME"]',
    });
    const spy = buildFetchSpy({ writes: [w1] });
    vi.stubGlobal("fetch", spy);

    renderPage();
    await userEvent.click(await screen.findByTestId("edit-1"));

    const tableInput = await screen.findByLabelText(/Tabla/i);
    await userEvent.clear(tableInput);
    await userEvent.type(tableInput, "public.orders_v2");

    await userEvent.click(
      screen.getByRole("button", { name: /Guardar cambios/i }),
    );

    await waitFor(() =>
      expect(findFetchCall(spy, "/sso-admin/write/update")).toBeDefined(),
    );
    const call = findFetchCall(spy, "/sso-admin/write/update")!;
    expect((call[1] as RequestInit).method).toBe("PUT");
    const body = JSON.parse((call[1] as RequestInit).body as string);
    expect(body.id).toBe(1);
    expect(body.tableName).toBe("public.orders_v2");
    // Existing chips render in the form state
    expect(body.columns).toBe('["ID","NAME"]');

    // drawer stays open in edit mode
    expect(screen.getByLabelText(/Tabla/i)).toBeInTheDocument();
  });

  it("chip add + remove inside the edit drawer (no fetch)", async () => {
    const w1 = mkWrite({
      id: 1,
      uuid: "smoke-write-1",
      columns: '["ID","NAME"]',
    });
    const spy = buildFetchSpy({ writes: [w1] });
    vi.stubGlobal("fetch", spy);

    renderPage();
    await userEvent.click(await screen.findByTestId("edit-1"));

    // Existing chips render
    expect(screen.getByTestId("write-columns-chip-0")).toHaveTextContent("ID");
    expect(screen.getByTestId("write-columns-chip-1")).toHaveTextContent("NAME");

    // Add a third chip
    await userEvent.type(
      screen.getByTestId("write-columns-input"),
      "EMAIL{Enter}",
    );
    expect(screen.getByTestId("write-columns-chip-2")).toHaveTextContent(
      "EMAIL",
    );

    // Remove middle chip
    await userEvent.click(screen.getByTestId("write-columns-remove-1"));
    expect(screen.getByTestId("write-columns-chip-0")).toHaveTextContent("ID");
    expect(screen.getByTestId("write-columns-chip-1")).toHaveTextContent(
      "EMAIL",
    );
    // No fetch yet — chips are pure local form state
    expect(spy.mock.calls.length).toBeLessThan(5);
  });

  it("Roles tab: toggling Vincular fires POST, Desvincular fires DELETE", async () => {
    const w1 = mkWrite({ id: 1, uuid: "smoke-write-1" });
    const roles: WriteRoleChecked[] = [
      mkRoleChecked({ roleId: 10, name: "ADMIN", checked: true }),
      mkRoleChecked({ roleId: 20, name: "USER", checked: false }),
    ];
    const spy = buildFetchSpy({
      writes: [w1],
      rolesForId: { 1: roles },
    });
    vi.stubGlobal("fetch", spy);

    renderPage();
    await userEvent.click(await screen.findByTestId("edit-1"));
    await userEvent.click(screen.getByRole("tab", { name: /^Roles$/i }));

    expect(await screen.findByText("ADMIN")).toBeInTheDocument();
    expect(screen.getByText("USER")).toBeInTheDocument();
    expect(screen.getByTestId("role-toggle-10")).toHaveTextContent(
      /Desvincular/i,
    );
    expect(screen.getByTestId("role-toggle-20")).toHaveTextContent(
      /Vincular/i,
    );

    // bind USER
    await userEvent.click(screen.getByTestId("role-toggle-20"));
    await waitFor(() =>
      expect(
        findFetchCall(spy, "/sso-admin/write/1/role/20"),
      ).toBeDefined(),
    );
    expect(
      (findFetchCall(spy, "/sso-admin/write/1/role/20")![1] as RequestInit)
        .method,
    ).toBe("POST");

    // unbind ADMIN
    await userEvent.click(screen.getByTestId("role-toggle-10"));
    await waitFor(() =>
      expect(
        findFetchCall(spy, "/sso-admin/write/1/role/10"),
      ).toBeDefined(),
    );
    expect(
      (findFetchCall(spy, "/sso-admin/write/1/role/10")![1] as RequestInit)
        .method,
    ).toBe("DELETE");
  });

  it("deleting opens the confirm modal and on confirm fires DELETE /write/{id}", async () => {
    const w1 = mkWrite({ id: 33, uuid: "doomed-write" });
    const spy = buildFetchSpy({ writes: [w1] });
    vi.stubGlobal("fetch", spy);

    renderPage();
    await userEvent.click(await screen.findByTestId("delete-33"));

    const dialog = await screen.findByRole("dialog", { name: /Eliminar write/i });
    expect(dialog).toBeInTheDocument();

    await userEvent.click(
      within(dialog).getByTestId("write-confirm-delete"),
    );

    await waitFor(() =>
      expect(findFetchCall(spy, "/sso-admin/write/33")).toBeDefined(),
    );
    const call = findFetchCall(spy, "/sso-admin/write/33")!;
    expect((call[1] as RequestInit).method).toBe("DELETE");
  });

  it("shows the empty-state copy when there are no writes", async () => {
    const spy = buildFetchSpy({ writes: [] });
    vi.stubGlobal("fetch", spy);

    renderPage();
    expect(
      await screen.findByText("Aún no hay writes."),
    ).toBeInTheDocument();
  });
});
