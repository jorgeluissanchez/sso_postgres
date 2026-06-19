import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Table, type Column } from "@/components/ui/Table";
import { useToast } from "@/components/ui/Toast";
import {
  useCreateUser,
  useUpdateUser,
  useUsers,
} from "@/hooks/useUsers";
import type { UserResponse } from "@/api/types";
import { UserFormDrawer } from "./UserFormDrawer";
import type { UserFormValues } from "@/schemas";

export function UsersListPage() {
  const users = useUsers();
  const createUser = useCreateUser();
  const updateUser = useUpdateUser();
  const toast = useToast();
  const [editing, setEditing] = useState<UserResponse | null>(null);
  const [creating, setCreating] = useState(false);

  async function handleSubmit(values: UserFormValues & { id?: number; password?: string | undefined }) {
    if (values.id) {
      await updateUser.mutateAsync({
        id: values.id,
        fullName: values.fullName,
        email: values.email,
        roleNames: values.roleNames,
        ...(values.password ? { password: values.password } : {}),
      });
      toast.show("Usuario actualizado", "success");
    } else {
      await createUser.mutateAsync({
        username: values.username,
        fullName: values.fullName,
        email: values.email,
        password: values.password ?? "",
        roleNames: values.roleNames,
      });
      toast.show("Usuario creado", "success");
    }
    setEditing(null);
    setCreating(false);
  }

  const columns: Column<UserResponse>[] = [
    { key: "username", header: "Usuario", render: (u) => u.username },
    { key: "fullName", header: "Nombre", render: (u) => u.fullName },
    { key: "email", header: "Email", render: (u) => u.email },
    {
      key: "active",
      header: "Estado",
      render: (u) =>
        u.active ? (
          <span className="rounded bg-emerald-100 px-2 py-0.5 text-xs text-emerald-800">
            Activo
          </span>
        ) : (
          <span className="rounded bg-slate-200 px-2 py-0.5 text-xs text-slate-700">
            Inactivo
          </span>
        ),
    },
    {
      key: "ldap",
      header: "Fuente",
      render: (u) => (u.ldap ? "LDAP" : "Local"),
    },
    {
      key: "roles",
      header: "Roles",
      render: (u) =>
        u.roleNames.length > 0 ? u.roleNames.join(", ") : "—",
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (u) => (
        <Button size="sm" variant="secondary" onClick={() => setEditing(u)}>
          Editar
        </Button>
      ),
    },
  ];

  return (
    <section>
      <header className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">Usuarios</h1>
        <Button onClick={() => setCreating(true)}>+ Nuevo usuario</Button>
      </header>

      <Table
        columns={columns}
        rows={users.data ?? []}
        rowKey={(u) => u.id}
        loading={users.isLoading}
        empty="Aún no hay usuarios."
      />

      <UserFormDrawer
        open={creating || editing !== null}
        user={editing}
        onClose={() => {
          setCreating(false);
          setEditing(null);
        }}
        onSubmit={handleSubmit}
      />
    </section>
  );
}
