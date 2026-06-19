import { useMemo } from "react";
import { Drawer } from "@/components/ui/Drawer";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { MultiSelect } from "@/components/ui/MultiSelect";
import { Form, zodFieldErrors } from "@/components/forms/Form";
import { useRoles } from "@/hooks/useRoles";
import {
  userFormSchema,
  type UserFormValues,
} from "@/schemas";
import type { UserResponse } from "@/api/types";

interface Props {
  open: boolean;
  user: UserResponse | null; // null = create
  onClose: () => void;
  onSubmit: (values: UserFormValues) => Promise<void>;
}

export function UserFormDrawer({ open, user, onClose, onSubmit }: Props) {
  const rolesQuery = useRoles();

  const initialValues: UserFormValues = useMemo(
    () => ({
      username: user?.username ?? "",
      fullName: user?.fullName ?? "",
      email: user?.email ?? "",
      password: "",
      roleNames: user?.roleNames ?? [],
    }),
    [user],
  );

  const roleOptions = useMemo(
    () =>
      (rolesQuery.data ?? []).map((r) => ({
        value: { id: r.id, name: r.name },
        label: r.name,
      })),
    [rolesQuery.data],
  );

  return (
    <Drawer
      open={open}
      onClose={onClose}
      title={user ? `Editar usuario: ${user.username}` : "Nuevo usuario"}
      description={
        user
          ? "Modifica los datos del usuario. La contraseña solo se actualiza si la escribes."
          : "Crea un usuario nuevo y asígnale uno o más roles."
      }
      footer={null}
    >
      <Form<UserFormValues>
        initialValues={initialValues}
        validate={(values) => {
          const result = userFormSchema.safeParse(values);
          if (result.success) return {};
          return zodFieldErrors(result.error);
        }}
        onSubmit={async (values) => {
          // For create: password is required. For edit: optional.
          const payload = user
            ? {
                id: user.id,
                fullName: values.fullName,
                email: values.email,
                roleNames: values.roleNames,
                ...(values.password ? { password: values.password } : {}),
              }
            : { ...values, password: values.password ?? "" };
          await onSubmit(payload as UserFormValues & { id?: number; password?: string | undefined });
        }}
        onCancel={onClose}
        submitLabel={user ? "Guardar cambios" : "Crear usuario"}
      >
        {({ values, setField, errors }) => (
          <>
            <Input
              label="Usuario"
              required
              value={values.username}
              onChange={(e) => setField("username", e.target.value)}
              error={errors.username}
              disabled={!!user}
              hint={user ? "El usuario no se puede cambiar" : undefined}
              autoComplete="off"
            />
            <div className="h-3" />
            <Input
              label="Nombre completo"
              required
              value={values.fullName}
              onChange={(e) => setField("fullName", e.target.value)}
              error={errors.fullName}
            />
            <div className="h-3" />
            <Input
              label="Email"
              required
              type="email"
              value={values.email}
              onChange={(e) => setField("email", e.target.value)}
              error={errors.email}
            />
            <div className="h-3" />
            <Input
              label={user ? "Nueva contraseña (opcional)" : "Contraseña"}
              required={!user}
              type="password"
              value={values.password ?? ""}
              onChange={(e) => setField("password", e.target.value)}
              error={errors.password}
              hint={
                user
                  ? "Déjala vacía para conservar la actual"
                  : "Mínimo 8 caracteres"
              }
              autoComplete="new-password"
            />
            <div className="h-3" />
            <label className="mb-1 block text-sm font-medium text-slate-700">
              Roles
            </label>
            <MultiSelect
              options={roleOptions}
              selectedIds={new Set(
                (rolesQuery.data ?? [])
                  .filter((r) => values.roleNames.includes(r.name))
                  .map((r) => r.id),
              )}
              onChange={(next) => {
                const names = (rolesQuery.data ?? [])
                  .filter((r) => next.has(r.id))
                  .map((r) => r.name);
                setField("roleNames", names);
              }}
              placeholder="Selecciona uno o más roles"
              emptyText={rolesQuery.isLoading ? "Cargando…" : "No hay roles"}
            />
            <div className="mt-6 flex justify-end gap-2">
              <Button type="button" variant="secondary" onClick={onClose}>
                Cancelar
              </Button>
              <Button type="submit">{user ? "Guardar" : "Crear"}</Button>
            </div>
          </>
        )}
      </Form>
    </Drawer>
  );
}
