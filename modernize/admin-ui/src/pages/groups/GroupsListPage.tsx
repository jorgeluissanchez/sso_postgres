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

  async function handleSubmit(values: GroupFormValues) {
    await createGroup.mutateAsync(values);
    toast.show("Grupo creado", "success");
    setCreating(false);
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
        open={creating}
        onClose={() => setCreating(false)}
        onSubmit={handleSubmit}
      />
    </section>
  );
}
