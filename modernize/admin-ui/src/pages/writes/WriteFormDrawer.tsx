import { useMemo } from "react";
import { ChipInput } from "@/components/ui/ChipInput";
import { Drawer } from "@/components/ui/Drawer";
import { Input } from "@/components/ui/Input";
import { Form, zodFieldErrors } from "@/components/forms/Form";
import { Tabs, type TabItem } from "@/components/ui/Tabs";
import { BindingTab } from "@/components/ui/BindingTab";
import { writeFormSchema, type WriteFormValues } from "@/schemas";
import { useMicroservices } from "@/hooks/useMicroservices";
import {
  useMicroserviceTables,
  type MicroserviceTableSource,
} from "@/hooks/useMicroserviceTables";
import type {
  MicroserviceResponse,
  WriteDefinitionRequest,
  WriteDefinitionResponse,
} from "@/api/types";
import {
  useBindWriteRole,
  useUnbindWriteRole,
  useWriteRolesChecked,
} from "@/hooks/useWrites";

/**
 * Drawer for create + edit of a WriteDefinition. Hosts 2 tabs:
 * <ol>
 *   <li><b>General</b> — uuid + writeType + microservice +
 *       tableName + columns + keyColumns. The only tab that
 *       submits a CRUD payload. Uses {@code <Form>} + zod,
 *       the {@code <ChipInput>} primitive for array fields,
 *       and a {@code <TablePicker>} panel that lights up when
 *       a {@code kind=QUERY} microservice is selected.</li>
 *   <li><b>Roles</b> — checked-listing with one Vincular /
 *       Desvincular toggle per role. Reuses
 *       {@link BindingTab} (the same primitive AppFormDrawer
 *       uses, parametrized via {@code listTestIdPrefix}).
 *       Available only in edit mode (the write must exist
 *       before it can be bound).</li>
 * </ol>
 *
 * <p>The drawer does NOT close after "Guardar cambios" in
 * edit mode — the admin usually wants to keep toggling
 * bindings without re-opening. Close is explicit (X button
 * or backdrop). Matches the {@code AppFormDrawer} pattern.
 *
 * <p>JSON-as-string bridging: {@code columns} / {@code
 * keyColumns} travel as JSON over the wire but live as
 * {@code string[]} in the form. The {@link parseColumns}
 * helper handles the response → initial-values direction;
 * {@code JSON.stringify} runs in the submit handler so
 * raw user input never has to deal with serialization.
 */
interface Props {
  open: boolean;
  write: WriteDefinitionResponse | null;
  onClose: () => void;
  onSubmit: (payload: WriteDefinitionRequest) => Promise<void>;
}

export function WriteFormDrawer({ open, write, onClose, onSubmit }: Props) {
  const initialValues: WriteFormValues = useMemo(
    () => ({
      uuid: write?.uuid ?? "",
      writeType: write?.writeType ?? "INSERT",
      microserviceId: write?.microserviceId ?? null,
      tableName: write?.tableName ?? "",
      columns: parseColumns(write?.columns),
      keyColumns: parseColumns(write?.keyColumns),
    }),
    [write],
  );

  const tabs: TabItem[] = [
    {
      key: "general",
      label: "General",
      content: (
        <GeneralTab
          write={write}
          initialValues={initialValues}
          onSubmit={onSubmit}
          onClose={onClose}
        />
      ),
    },
    {
      key: "roles",
      label: "Roles",
      content: write ? (
        <RolesTab writeId={write.id} />
      ) : (
        <DisabledTabHint />
      ),
    },
  ];

  return (
    <Drawer
      open={open}
      onClose={onClose}
      title={write ? `Editar write: ${write.uuid}` : "Nuevo write"}
      description="Define un INSERT o UPDATE para query-service."
      footer={null}
      width="lg"
    >
      <Tabs tabs={tabs} ariaLabel="Secciones del formulario de Write" />
    </Drawer>
  );
}

/* ====================== General tab ====================== */

interface GeneralTabProps {
  write: WriteDefinitionResponse | null;
  initialValues: WriteFormValues;
  onSubmit: (payload: WriteDefinitionRequest) => Promise<void>;
  onClose: () => void;
}

function GeneralTab({
  write,
  initialValues,
  onSubmit,
  onClose,
}: GeneralTabProps) {
  const services = useMicroservices();
  const writeInstances = useMemo(
    // Only QUERY rows can serve writes — REST has no SQL
    // executor to even attempt the request. Mirrors the
    // filter in QueryFormDrawer.
    () => (services.data ?? []).filter((m) => m.kind === "QUERY"),
    [services.data],
  );

  return (
    <Form<WriteFormValues>
      initialValues={initialValues}
      validate={(values) => {
        const result = writeFormSchema.safeParse(values);
        if (result.success) return {};
        return zodFieldErrors(result.error);
      }}
      onSubmit={async (values) => {
        // Bridge from form-shape (`string[]`) to wire-shape
        // (JSON-as-string). `keyColumns` drops to `null` when
        // empty so the backend's nullable column stays empty
        // instead of receiving the literal string `"[]"`.
        // `microserviceId` is `number | null` already; the
        // schema's transform coerces empty string back to
        // `null` so the legacy "global" semantic lives.
        const wireValues = {
          columns: JSON.stringify(values.columns),
          keyColumns:
            values.keyColumns.length > 0
              ? JSON.stringify(values.keyColumns)
              : null,
          microserviceId: values.microserviceId,
        };
        const payload: WriteDefinitionRequest = write
          ? { id: write.id, uuid: values.uuid, writeType: values.writeType, ...wireValues, tableName: values.tableName }
          : { uuid: values.uuid, writeType: values.writeType, ...wireValues, tableName: values.tableName };
        await onSubmit(payload);
      }}
      onCancel={onClose}
      submitLabel={write ? "Guardar cambios" : "Crear"}
    >
      {({ values, setField, errors }) => {
        const selectedMs =
          writeInstances.find((m) => m.id === values.microserviceId) ?? null;
        return (
          <>
            <Input
              label="UUID"
              required
              value={values.uuid}
              onChange={(e) => setField("uuid", e.target.value)}
              error={errors.uuid}
              hint="Handle público (sin espacios)"
            />
            <div className="h-3" />
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">
                Tipo
                <span aria-hidden="true" className="ml-0.5 text-red-600">
                  *
                </span>
              </label>
              <select
                value={values.writeType}
                onChange={(e) =>
                  setField("writeType", e.target.value as "INSERT" | "UPDATE")
                }
                aria-invalid={errors.writeType ? true : undefined}
                className={[
                  "w-full rounded border bg-white px-3 py-2 text-sm outline-none",
                  errors.writeType
                    ? "border-red-400"
                    : "border-slate-300 focus:border-sky-500",
                ].join(" ")}
                data-testid="write-type"
              >
                <option value="INSERT">INSERT</option>
                <option value="UPDATE">UPDATE</option>
              </select>
              {errors.writeType ? (
                <p role="alert" className="mt-1 text-xs text-red-600">
                  {errors.writeType}
                </p>
              ) : null}
            </div>
            <div className="h-3" />
            <div>
              <label
                htmlFor="write-microservice"
                className="mb-1 block text-sm font-medium text-slate-700"
              >
                Microservicio (kind=QUERY)
              </label>
              <select
                id="write-microservice"
                value={
                  values.microserviceId == null
                    ? ""
                    : String(values.microserviceId)
                }
                onChange={(e) =>
                  setField(
                    "microserviceId",
                    e.target.value === "" ? null : Number(e.target.value),
                  )
                }
                aria-invalid={errors.microserviceId ? true : undefined}
                className="w-full rounded border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-sky-500 focus:ring-1 focus:ring-sky-500"
                data-testid="write-microservice"
              >
                <option value="">Sin binding (global)</option>
                {writeInstances.map((m) => (
                  <option key={m.id} value={String(m.id)}>
                    #{m.id} · {m.instanceName ?? m.serviceId}
                    {m.dialect ? ` (${m.dialect})` : ""}
                  </option>
                ))}
              </select>
              {writeInstances.length === 0 && !services.isLoading ? (
                <p className="mt-1 text-[11px] text-slate-500">
                  No hay microservicios QUERY aprovisionados aún.
                </p>
              ) : null}
            </div>
            <div className="h-3" />
            <Input
              label="Tabla"
              required
              value={values.tableName}
              onChange={(e) => setField("tableName", e.target.value)}
              error={errors.tableName}
              hint="schema.table (ej: public.users)"
            />
            {selectedMs ? (
              <>
                <div className="h-2" />
                <TablePicker
                  microservice={selectedMs}
                  onPick={(qualified) => setField("tableName", qualified)}
                />
              </>
            ) : null}
            <div className="h-3" />
            <ChipInput
              label="Columnas"
              required
              value={values.columns}
              onChange={(v) => setField("columns", v)}
              error={errors.columns}
              placeholder="Añadir columna + Enter"
              dataTestId="write-columns"
            />
            <div className="h-3" />
            <ChipInput
              label="Columnas clave (opcional)"
              value={values.keyColumns}
              onChange={(v) => setField("keyColumns", v)}
              hint="Solo para UPDATE"
              dataTestId="write-key-columns"
            />
          </>
        );
      }}
    </Form>
  );
}

/**
 * Inline helper for the General tab. Renders a small panel
 * that lights up after the operator picks a {@code kind=QUERY}
 * microservice and offers the live table catalog pulled from
 * {@code GET /query-service-<instance>/tables?dialect=…}.
 *
 * <p>Choosing a row from the dropdown autofills the parent
 * form's {@code tableName} input with the qualified
 * {@code schema.name} string. The text input stays
 * user-editable — the picker is a UX win, not a constraint
 * (an admin can still override with {@code custom_table}
 * values if needed).
 */
function TablePicker({
  microservice,
  onPick,
}: {
  microservice: MicroserviceResponse;
  onPick: (qualified: string) => void;
}) {
  const source: MicroserviceTableSource | null = microservice.instanceName
    ? {
        instanceName: microservice.instanceName,
        dialect: microservice.dialect ?? microservice.instanceName,
      }
    : microservice.dialect
      ? { instanceName: microservice.dialect, dialect: microservice.dialect }
      : null;

  const tables = useMicroserviceTables(source);

  if (!source) {
    return (
      <p className="rounded border border-slate-200 bg-slate-50 px-3 py-2 text-[11px] text-slate-500">
        El microservicio seleccionado no tiene instanceName ni
        dialect configurado — no se puede cargar el catálogo.
      </p>
    );
  }

  return (
    <div
      className="rounded border border-slate-200 bg-slate-50 p-3"
      data-testid="write-table-picker"
    >
      <div className="flex items-center justify-between">
        <span className="text-xs font-medium text-slate-700">
          Tablas en {source.instanceName}
          {source.dialect ? (
            <span className="ml-1 text-[10px] text-slate-500">
              ({source.dialect})
            </span>
          ) : null}
        </span>
        {tables.isFetching ? (
          <span className="text-[10px] text-slate-500">cargando…</span>
        ) : null}
      </div>
      {tables.isError ? (
        <p className="mt-1 text-xs text-red-600" data-testid="write-table-picker-error">
          No se pudo cargar el catálogo de tablas.
        </p>
      ) : null}
      {tables.data && tables.data.length > 0 ? (
        <select
          aria-label="Selector de catálogo del microservicio"
          onChange={(e) => {
            const t = tables.data!.find(
              (x) => `${x.schema}.${x.name}` === e.target.value,
            );
            if (t) onPick(`${t.schema}.${t.name}`);
          }}
          defaultValue=""
          className="mt-2 w-full rounded border border-slate-300 bg-white px-2 py-1.5 text-sm"
          data-testid="write-table-picker-select"
        >
          <option value="">— elegir —</option>
          {tables.data.map((t) => (
            <option
              key={`${t.schema ?? ""}.${t.name}`}
              value={`${t.schema ?? ""}.${t.name}`}
            >
              {t.schema ? `${t.schema}.` : ""}
              {t.name}
            </option>
          ))}
        </select>
      ) : tables.isFetched ? (
        <p className="mt-1 text-[11px] text-slate-500">
          El datasource no expone tablas base con este dialect.
        </p>
      ) : null}
    </div>
  );
}

function DisabledTabHint() {
  return (
    <p className="text-sm text-slate-500">
      Guarda el write primero para habilitar la gestión de vinculaciones.
    </p>
  );
}

/* ====================== Roles tab ====================== */

function RolesTab({ writeId }: { writeId: number }) {
  const roles = useWriteRolesChecked(writeId);
  const bind = useBindWriteRole();
  const unbind = useUnbindWriteRole();
  const pending = bind.isPending || unbind.isPending;
  return (
    <BindingTab
      entityId={writeId}
      listTestIdPrefix="write-bindings"
      data={roles.data}
      isLoading={roles.isLoading}
      isPending={pending}
      emptyText="No hay roles creados."
      toggleIdPrefix="role-toggle"
      getRowId={(r) => r.roleId}
      getRowChecked={(r) => r.checked}
      onToggle={(roleId, checked) => {
        if (checked) {
          void unbind.mutateAsync({ id: writeId, roleId });
        } else {
          void bind.mutateAsync({ id: writeId, roleId });
        }
      }}
      renderRow={(r) => r.name}
    />
  );
}

/* ====================== helpers ====================== */

/**
 * Tolerant {@code JSON.parse} for the {@code columns} /
 * {@code keyColumns} wire fields. The backend stores them
 * as {@code "[\"ID\",\"NAME\"]"} but a defensive parse keeps
 * a corrupt row from crashing the whole form (an empty
 * chip list is a much better failure than a white screen).
 */
function parseColumns(s: string | null | undefined): string[] {
  if (!s) return [];
  try {
    const v = JSON.parse(s);
    return Array.isArray(v) ? v.map(String) : [];
  } catch {
    return [];
  }
}
