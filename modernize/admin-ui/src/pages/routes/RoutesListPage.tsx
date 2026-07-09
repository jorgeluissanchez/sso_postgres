import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { Table, type Column } from "@/components/ui/Table";
import { useToast } from "@/components/ui/Toast";
import {
  useCreateRoute,
  useDeleteRoute,
  useRoutes,
  useUpdateRoute,
} from "@/hooks/useRoutes";
import type { RouteResponse } from "@/api/types";
import { RouteFormDrawer } from "./RouteFormDrawer";
import type { RouteFormValues } from "@/schemas";

export function RoutesListPage() {
  const routes = useRoutes();
  const createRoute = useCreateRoute();
  const updateRoute = useUpdateRoute();
  const deleteRoute = useDeleteRoute();
  const toast = useToast();
  const [editing, setEditing] = useState<RouteResponse | null>(null);
  const [creating, setCreating] = useState(false);
  const [deleting, setDeleting] = useState<RouteResponse | null>(null);

  async function handleSubmit(values: RouteFormValues & { id?: number }) {
    if (values.id) {
      await updateRoute.mutateAsync({
        id: values.id,
        name: values.name,
        icon: values.icon,
        path: values.path,
        menuOrder: values.menuOrder,
        type: values.type,
        idParent: values.idParent,
      });
      toast.show("Ruta actualizada", "success");
    } else {
      await createRoute.mutateAsync({
        name: values.name,
        icon: values.icon,
        path: values.path,
        menuOrder: values.menuOrder,
        type: values.type,
        idParent: values.idParent,
      });
      toast.show("Ruta creada", "success");
    }
    setEditing(null);
    setCreating(false);
  }

  async function confirmDelete() {
    if (!deleting) return;
    await deleteRoute.mutateAsync(deleting.id);
    toast.show("Ruta eliminada", "success");
    setDeleting(null);
  }

  const rows = (routes.data ?? []).slice().sort((a, b) => {
    if (a.idParent == null && b.idParent != null) return -1;
    if (a.idParent != null && b.idParent == null) return 1;
    return a.menuOrder - b.menuOrder;
  });

  const columns: Column<RouteResponse>[] = [
    { key: "name", header: "Nombre", render: (r) => r.name },
    { key: "path", header: "Path", render: (r) => <code className="text-xs">{r.path}</code> },
    { key: "type", header: "Tipo", render: (r) => r.type },
    {
      key: "parent",
      header: "Padre",
      render: (r) => {
        if (r.idParent == null) return "—";
        const parent = (routes.data ?? []).find((x) => x.id === r.idParent);
        return parent?.name ?? `#${r.idParent}`;
      },
    },
    {
      key: "order",
      header: "Orden",
      align: "right",
      render: (r) => r.menuOrder,
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (r) => (
        <div className="flex justify-end gap-2">
          <Button
            size="sm"
            variant="secondary"
            onClick={() => setEditing(r)}
            data-testid={`edit-${r.id}`}
          >
            Editar
          </Button>
          <Button
            size="sm"
            variant="danger"
            onClick={() => setDeleting(r)}
            data-testid={`delete-${r.id}`}
          >
            Eliminar
          </Button>
        </div>
      ),
    },
  ];

  return (
    <section>
      <header className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">Rutas</h1>
        <Button onClick={() => setCreating(true)} data-testid="new-route">
          + Nueva ruta
        </Button>
      </header>
      <Table
        columns={columns}
        rows={rows}
        rowKey={(r) => r.id}
        loading={routes.isLoading}
        empty="Aún no hay rutas."
      />
      <RouteFormDrawer
        open={creating || editing !== null}
        route={editing}
        parentCandidates={routes.data ?? []}
        onClose={() => {
          setCreating(false);
          setEditing(null);
        }}
        onSubmit={handleSubmit}
      />
      <Modal
        open={deleting !== null}
        onClose={() => setDeleting(null)}
        title="Eliminar ruta"
        description={`¿Seguro que quieres eliminar "${deleting?.name}"?`}
        footer={
          <>
            <Button variant="secondary" onClick={() => setDeleting(null)}>
              Cancelar
            </Button>
            <Button
              variant="danger"
              loading={deleteRoute.isPending}
              onClick={() => void confirmDelete()}
            >
              Eliminar
            </Button>
          </>
        }
      >
        <p className="text-sm text-slate-600">
          Las rutas hijas quedarán huérfanas.
        </p>
      </Modal>
    </section>
  );
}
