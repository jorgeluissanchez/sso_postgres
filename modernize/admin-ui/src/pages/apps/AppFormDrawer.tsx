import { useMemo, type ReactNode } from "react";
import { Button } from "@/components/ui/Button";
import { Drawer } from "@/components/ui/Drawer";
import { Input } from "@/components/ui/Input";
import { Form, zodFieldErrors } from "@/components/forms/Form";
import { Tabs, type TabItem } from "@/components/ui/Tabs";
import { appFormSchema, type AppFormValues } from "@/schemas";
import type { AppResponse } from "@/api/types";
import {
  useAppMicroservicesChecked,
  useAppRolesChecked,
  useAppRoutesChecked,
  useAppUsersChecked,
  useBindAppMicroservice,
  useBindAppRole,
  useBindAppRoute,
  useBindAppUser,
  useUnbindAppMicroservice,
  useUnbindAppRole,
  useUnbindAppRoute,
  useUnbindAppUser,
} from "@/hooks/useApps";

/**
 * Drawer for create + edit of an App. Hosts 5 tabs:
 * <ol>
 *   <li><b>General</b> — name + description, the only tab that
 *       submits a CRUD payload. Uses the project's hand-rolled
 *       {@code Form} + zod.</li>
 *   <li><b>Roles / Usuarios / Rutas / Microservicios</b> —
 *       checked-listings with one Vincular/Desvincular toggle
 *       per row. Each toggle is one HTTP round-trip (matches
 *       the {@code RolesBindingModal} pattern in
 *       {@code QueriesAdminPage.tsx}). Available only in edit
 *       mode (an app must exist before it can be bound).</li>
 * </ol>
 *
 * <p>The drawer does NOT close after "Guardar cambios" in
 * edit mode — the admin usually wants to keep toggling
 * bindings without re-opening. Close is explicit (X button or
 * backdrop).
 */
interface Props {
  open: boolean;
  app: AppResponse | null;
  onClose: () => void;
  onSubmit: (values: AppFormValues & { id?: number }) => Promise<void>;
}

export function AppFormDrawer({ open, app, onClose, onSubmit }: Props) {
  const initialValues: AppFormValues = useMemo(
    () => ({
      name: app?.name ?? "",
      description: app?.description ?? "",
    }),
    [app],
  );

  const tabs: TabItem[] = [
    {
      key: "general",
      label: "General",
      content: (
        <GeneralTab
          app={app}
          initialValues={initialValues}
          onSubmit={onSubmit}
          onClose={onClose}
        />
      ),
    },
    {
      key: "roles",
      label: "Roles",
      content: app ? <RolesTab appId={app.id} /> : <DisabledTabHint />,
    },
    {
      key: "users",
      label: "Usuarios",
      content: app ? <UsersTab appId={app.id} /> : <DisabledTabHint />,
    },
    {
      key: "routes",
      label: "Rutas",
      content: app ? <RoutesTab appId={app.id} /> : <DisabledTabHint />,
    },
    {
      key: "microservices",
      label: "Microservicios",
      content: app ? (
        <MicroservicesTab appId={app.id} />
      ) : (
        <DisabledTabHint />
      ),
    },
  ];

  return (
    <Drawer
      open={open}
      onClose={onClose}
      title={app ? `Editar app: ${app.name}` : "Nueva app"}
      description="Define nombre/descripción y vincula entidades permitidas."
      footer={null}
      width="lg"
    >
      <Tabs tabs={tabs} ariaLabel="Secciones del formulario de App" />
    </Drawer>
  );
}

/* ====================== General tab ====================== */

interface GeneralTabProps {
  app: AppResponse | null;
  initialValues: AppFormValues;
  onSubmit: (values: AppFormValues & { id?: number }) => Promise<void>;
  onClose: () => void;
}

function GeneralTab({ app, initialValues, onSubmit, onClose }: GeneralTabProps) {
  return (
    <Form<AppFormValues>
      initialValues={initialValues}
      validate={(values) => {
        const result = appFormSchema.safeParse(values);
        if (result.success) return {};
        return zodFieldErrors(result.error);
      }}
      onSubmit={async (values) => {
        await onSubmit(app ? { id: app.id, ...values } : values);
      }}
      onCancel={onClose}
      submitLabel={app ? "Guardar cambios" : "Crear"}
    >
      {({ values, setField, errors }) => (
        <>
          <Input
            label="Nombre"
            required
            value={values.name}
            onChange={(e) => setField("name", e.target.value)}
            error={errors.name}
            hint="Único en el sistema"
          />
          <div className="h-3" />
          <Input
            label="Descripción"
            value={values.description ?? ""}
            onChange={(e) => setField("description", e.target.value)}
            error={errors.description}
          />
        </>
      )}
    </Form>
  );
}

function DisabledTabHint() {
  return (
    <p className="text-sm text-slate-500">
      Guarda la app primero para habilitar la gestión de vinculaciones.
    </p>
  );
}

/* ====================== Binding tabs ====================== */

/**
 * The four binding tabs share the same shape: a list of rows
 * with a {@code checked: boolean} flag, each with a Vincular /
 * Desvincular button. We parameterize the differences — the
 * checked-hook, the bind/unbind hooks, the testid prefix, how
 * to extract the row's id (which lives under different field
 * names per family: {@code roleId}, {@code userId},
 * {@code routeId}, {@code microserviceId}), and how to render
 * the row's identifying label — so the actual tab components
 * stay tiny.
 */
interface BindingTabProps<TRow> {
  appId: number;
  data: TRow[] | undefined;
  isLoading: boolean;
  isPending: boolean;
  emptyText: string;
  toggleIdPrefix: string;
  getRowId: (row: TRow) => number;
  getRowChecked: (row: TRow) => boolean;
  onToggle: (rowId: number, currentlyBound: boolean) => void;
  renderRow: (row: TRow) => ReactNode;
}

function BindingTab<TRow>({
  appId,
  data,
  isLoading,
  isPending,
  emptyText,
  toggleIdPrefix,
  getRowId,
  getRowChecked,
  onToggle,
  renderRow,
}: BindingTabProps<TRow>) {
  return (
    <ul className="divide-y divide-slate-200" data-testid={`app-bindings-${appId}`}>
      {(data ?? []).map((row) => {
        const rowId = getRowId(row);
        const checked = getRowChecked(row);
        return (
          <li
            key={rowId}
            className="flex items-center justify-between py-2 text-sm"
          >
            <span className="font-medium text-slate-800">{renderRow(row)}</span>
            <Button
              size="sm"
              variant={checked ? "secondary" : "primary"}
              disabled={isPending}
              loading={isPending}
              onClick={() => onToggle(rowId, checked)}
              data-testid={`${toggleIdPrefix}-${rowId}`}
            >
              {checked ? "Desvincular" : "Vincular"}
            </Button>
          </li>
        );
      })}
      {!isLoading && data && data.length === 0 ? (
        <li className="py-2 text-sm text-slate-500">{emptyText}</li>
      ) : null}
    </ul>
  );
}

function RolesTab({ appId }: { appId: number }) {
  const roles = useAppRolesChecked(appId);
  const bind = useBindAppRole();
  const unbind = useUnbindAppRole();
  const pending = bind.isPending || unbind.isPending;
  return (
    <BindingTab
      appId={appId}
      data={roles.data}
      isLoading={roles.isLoading}
      isPending={pending}
      emptyText="No hay roles creados."
      toggleIdPrefix="role-toggle"
      getRowId={(r) => r.roleId}
      getRowChecked={(r) => r.checked}
      onToggle={(roleId, checked) => {
        if (checked) {
          void unbind.mutateAsync({ id: appId, roleId });
        } else {
          void bind.mutateAsync({ id: appId, roleId });
        }
      }}
      renderRow={(r) => r.name}
    />
  );
}

function UsersTab({ appId }: { appId: number }) {
  const users = useAppUsersChecked(appId);
  const bind = useBindAppUser();
  const unbind = useUnbindAppUser();
  const pending = bind.isPending || unbind.isPending;
  return (
    <BindingTab
      appId={appId}
      data={users.data}
      isLoading={users.isLoading}
      isPending={pending}
      emptyText="No hay usuarios creados."
      toggleIdPrefix="user-toggle"
      getRowId={(u) => u.userId}
      getRowChecked={(u) => u.checked}
      onToggle={(userId, checked) => {
        if (checked) {
          void unbind.mutateAsync({ id: appId, userId });
        } else {
          void bind.mutateAsync({ id: appId, userId });
        }
      }}
      renderRow={(u) => u.username}
    />
  );
}

function RoutesTab({ appId }: { appId: number }) {
  const routes = useAppRoutesChecked(appId);
  const bind = useBindAppRoute();
  const unbind = useUnbindAppRoute();
  const pending = bind.isPending || unbind.isPending;
  return (
    <BindingTab
      appId={appId}
      data={routes.data}
      isLoading={routes.isLoading}
      isPending={pending}
      emptyText="No hay rutas creadas."
      toggleIdPrefix="route-toggle"
      getRowId={(r) => r.routeId}
      getRowChecked={(r) => r.checked}
      onToggle={(routeId, checked) => {
        if (checked) {
          void unbind.mutateAsync({ id: appId, routeId });
        } else {
          void bind.mutateAsync({ id: appId, routeId });
        }
      }}
      renderRow={(r) => (
        <span>
          {r.name}
          <span className="ml-2 text-xs text-slate-500">{r.path}</span>
        </span>
      )}
    />
  );
}

function MicroservicesTab({ appId }: { appId: number }) {
  const services = useAppMicroservicesChecked(appId);
  const bind = useBindAppMicroservice();
  const unbind = useUnbindAppMicroservice();
  const pending = bind.isPending || unbind.isPending;
  return (
    <BindingTab
      appId={appId}
      data={services.data}
      isLoading={services.isLoading}
      isPending={pending}
      emptyText="No hay microservicios creados."
      toggleIdPrefix="microservice-toggle"
      getRowId={(m) => m.microserviceId}
      getRowChecked={(m) => m.checked}
      onToggle={(microserviceId, checked) => {
        if (checked) {
          void unbind.mutateAsync({ id: appId, microserviceId });
        } else {
          void bind.mutateAsync({ id: appId, microserviceId });
        }
      }}
      renderRow={(m) => (
        <span>
          <span className="font-mono">{m.serviceId}</span>
          <span
            className={[
              "ml-2 rounded px-1.5 py-0.5 text-[10px] font-medium",
              m.kind === "QUERY"
                ? "bg-violet-50 text-violet-700"
                : "bg-slate-100 text-slate-700",
            ].join(" ")}
          >
            {m.kind}
          </span>
        </span>
      )}
    />
  );
}