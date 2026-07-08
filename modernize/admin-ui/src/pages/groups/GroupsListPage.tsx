import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Table, type Column } from "@/components/ui/Table";
import { useToast } from "@/components/ui/Toast";
import { useCreateGroup, useGroups } from "@/hooks/useGroups";
import type { GroupResponse } from "@/api/types";
import { GroupFormDrawer } from "./GroupFormDrawer";
import type { GroupFormValues } from "@/schemas";

export function GroupsListPage() {
  const groups = useGroups();
  const createGroup = useCreateGroup();
  const toast = useToast();
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<GroupResponse | null>(null);

  function closeDrawer() {
    setCreating(false);
    setEditing(null);
  }

  async function handleSubmit(values: GroupFormValues & { id?: number }) {
    const { id: _id, ...body } = values;
    await createGroup.mutateAsync(body);
    toast.show(editing ? "Grupo actualizado" : "Grupo creado", "success");
    closeDrawer();
  }

  const columns: Column<GroupResponse>[] = [
    { key: "name", header: "Nombre", render: (g) => g.name },
    { key: "description", header: "Descripción", render: (g) => g.description || "—" },
    {
      key: "members",
      header: "Miembros",
      align: "right",
      render: (g) => g.memberCount,
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (g) => (
        <Button size="sm" variant="secondary" onClick={() => setEditing(g)}>
          Editar
        </Button>
      ),
    },
  ];

  return (
    <section>
      <header className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">Grupos</h1>
        <Button onClick={() => setCreating(true)}>+ Nuevo grupo</Button>
      </header>
      <Table
        columns={columns}
        rows={groups.data ?? []}
        rowKey={(g) => g.id}
        loading={groups.isLoading}
        empty="Aún no hay grupos."
      />
      <GroupFormDrawer
        open={creating || editing !== null}
        group={editing}
        onClose={closeDrawer}
        onSubmit={handleSubmit}
      />
    </section>
  );
}
