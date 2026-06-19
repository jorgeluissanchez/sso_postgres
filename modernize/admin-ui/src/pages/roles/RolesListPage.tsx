import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Table, type Column } from "@/components/ui/Table";
import { useToast } from "@/components/ui/Toast";
import { useCreateRole, useRoles, useUpdateRole } from "@/hooks/useRoles";
import type { RoleResponse } from "@/api/types";
import { RoleFormDrawer } from "./RoleFormDrawer";
import type { RoleFormValues } from "@/schemas";

export function RolesListPage() {
  const roles = useRoles();
  const createRole = useCreateRole();
  const updateRole = useUpdateRole();
  const toast = useToast();
  const [editing, setEditing] = useState<RoleResponse | null>(null);
  const [creating, setCreating] = useState(false);

  async function handleSubmit(values: RoleFormValues & { id?: number }) {
    if (values.id) {
      await updateRole.mutateAsync({
        id: values.id,
        name: values.name,
        description: values.description,
      });
      toast.show("Rol actualizado", "success");
    } else {
      await createRole.mutateAsync({
        name: values.name,
        description: values.description,
      });
      toast.show("Rol creado", "success");
    }
    setEditing(null);
    setCreating(false);
  }

  const columns: Column<RoleResponse>[] = [
    { key: "name", header: "Nombre", render: (r) => r.name },
    { key: "description", header: "Descripción", render: (r) => r.description || "—" },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (r) => (
        <Button size="sm" variant="secondary" onClick={() => setEditing(r)}>
          Editar
        </Button>
      ),
    },
  ];

  return (
    <section>
      <header className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">Roles</h1>
        <Button onClick={() => setCreating(true)}>+ Nuevo rol</Button>
      </header>
      <Table
        columns={columns}
        rows={roles.data ?? []}
        rowKey={(r) => r.id}
        loading={roles.isLoading}
        empty="Aún no hay roles."
      />
      <RoleFormDrawer
        open={creating || editing !== null}
        role={editing}
        onClose={() => {
          setCreating(false);
          setEditing(null);
        }}
        onSubmit={handleSubmit}
      />
    </section>
  );
}
