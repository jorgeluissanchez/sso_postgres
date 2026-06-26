import { useMemo } from "react";
import { Drawer } from "@/components/ui/Drawer";
import { Input } from "@/components/ui/Input";
import { RadioGroup } from "@/components/ui/RadioGroup";
import { Form, zodFieldErrors } from "@/components/forms/Form";
import {
  microserviceFormSchema,
  QUERY_DIALECTS,
  type MicroserviceFormValues,
} from "@/schemas";
import type { MicroserviceResponse } from "@/api/types";

interface Props {
  open: boolean;
  microservice: MicroserviceResponse | null;
  onClose: () => void;
  onSubmit: (values: MicroserviceFormValues & { id?: number }) => Promise<void>;
}

/**
 * Form drawer for microservice CRUD. Two flavours:
 * <ul>
 *   <li>{@code kind="REST"} (default) — the legacy routing
 *       rule. The host/port/path triumvirate is required
 *       and drives the gateway's static route.</li>
 *   <li>{@code kind="QUERY"} — fills the additional JDBC
 *       metadata block. Backend creates the row, calls the
 *       provisioner sidecar to spin up a container, and
 *       waits for Eureka before returning. The form stays
 *       open with a spinner for that round-trip.</li>
 * </ul>
 *
 * <p>The QUERY block renders only when {@code values.kind}
 * flips to QUERY; for REST rows the QUERY fields are
 * hidden AND stripped from the payload (we don't send
 * them so the backend doesn't accidentally re-validate
 * them).
 */
export function MicroserviceFormDrawer({
  open,
  microservice,
  onClose,
  onSubmit,
}: Props) {
  const initialValues: MicroserviceFormValues = useMemo(
    () => ({
      serviceId: microservice?.serviceId ?? "",
      description: microservice?.description ?? "",
      requestUri: microservice?.requestUri ?? "",
      targetUriPath: microservice?.targetUriPath ?? "",
      targetUrlHost: microservice?.targetUrlHost ?? "",
      targetUrlPort: microservice?.targetUrlPort ?? "",
      kind: microservice?.kind ?? "REST",
      dialect: microservice?.dialect ?? "",
      jdbcUrl: microservice?.jdbcUrl ?? "",
      dbUsername: microservice?.dbUsername ?? "",
      dbPassword: microservice?.dbPassword ?? "",
      poolSize: microservice?.poolSize ?? 10,
      instanceName: microservice?.instanceName ?? "",
    }),
    [microservice],
  );

  return (
    <Drawer
      open={open}
      onClose={onClose}
      title={
        microservice
          ? `Editar microservicio: ${microservice.serviceId}`
          : "Nuevo microservicio"
      }
      description={
        initialValues.kind === "QUERY"
          ? "Configura los datos JDBC y el provisioner levantará un contenedor query-service."
          : "Configura el destino HTTP al que el gateway enrutará las peticiones."
      }
      footer={null}
      width="lg"
    >
      <Form<MicroserviceFormValues>
        initialValues={initialValues}
        validate={(values) => {
          const result = microserviceFormSchema.safeParse(values);
          if (result.success) return {};
          return zodFieldErrors(result.error);
        }}
        onSubmit={async (values) => {
          // Strip QUERY-only fields when the kind is REST so
          // we don't accidentally round-trip empty strings
          // for REST rows.
          const payload = values.kind === "QUERY"
            ? values
            : {
                serviceId: values.serviceId,
                description: values.description,
                requestUri: values.requestUri,
                targetUriPath: values.targetUriPath,
                targetUrlHost: values.targetUrlHost,
                targetUrlPort: values.targetUrlPort,
              };
          await onSubmit(microservice ? { id: microservice.id, ...payload } : payload);
        }}
        onCancel={onClose}
        submitLabel={microservice ? "Guardar cambios" : "Crear"}
      >
        {({ values, setField, errors }) => (
          <>
            <RadioGroup
              name="kind"
              label="Tipo de microservicio"
              value={values.kind}
              onChange={(v) => setField("kind", v)}
              error={errors.kind}
              options={[
                {
                  value: "REST",
                  label: "REST routing",
                  description: "Regla clásica de gateway (host + port + path).",
                },
                {
                  value: "QUERY",
                  label: "Query service",
                  description:
                    "Levanta un contenedor query-service contra un backing datasource.",
                },
              ]}
            />

            <div className="h-4" />

            <div className="grid grid-cols-2 gap-3">
              <Input
                label="Service ID"
                required
                value={values.serviceId}
                onChange={(e) => setField("serviceId", e.target.value)}
                error={errors.serviceId}
                hint="Identificador único (lowercase, sin espacios)"
              />
              <Input
                label="Request URI"
                required
                value={values.requestUri}
                onChange={(e) => setField("requestUri", e.target.value)}
                error={errors.requestUri}
                hint="Patrón de URI entrante, ej. /api/orders/**"
              />
            </div>
            <div className="h-3" />
            <Input
              label="Descripción"
              value={values.description}
              onChange={(e) => setField("description", e.target.value)}
              error={errors.description}
            />
            <div className="h-3" />

            {values.kind === "REST" ? (
              <div className="grid grid-cols-3 gap-3">
                <Input
                  label="Target host"
                  required
                  value={values.targetUrlHost}
                  onChange={(e) => setField("targetUrlHost", e.target.value)}
                  error={errors.targetUrlHost}
                />
                <Input
                  label="Target port"
                  required
                  value={values.targetUrlPort}
                  onChange={(e) => setField("targetUrlPort", e.target.value)}
                  error={errors.targetUrlPort}
                />
                <Input
                  label="Target path"
                  required
                  value={values.targetUriPath}
                  onChange={(e) => setField("targetUriPath", e.target.value)}
                  error={errors.targetUriPath}
                />
              </div>
            ) : (
              <>
                {/* QUERY block: JDBC metadata + instance name. */}
                <div className="grid grid-cols-2 gap-3">
                  <DialectSelect
                    value={values.dialect}
                    onChange={(v) => setField("dialect", v)}
                    error={errors.dialect}
                  />
                  <Input
                    label="Instance name"
                    required
                    value={values.instanceName}
                    onChange={(e) => setField("instanceName", e.target.value)}
                    error={errors.instanceName}
                    hint="Sufijo del service-id en Eureka. ej. 'oracle-dev'"
                  />
                </div>
                <div className="h-3" />
                <Input
                  label="JDBC URL"
                  required
                  value={values.jdbcUrl}
                  onChange={(e) => setField("jdbcUrl", e.target.value)}
                  error={errors.jdbcUrl}
                  hint="ej. jdbc:postgresql://host:5432/db"
                />
                <div className="h-3" />
                <div className="grid grid-cols-2 gap-3">
                  <Input
                    label="DB username"
                    required
                    value={values.dbUsername}
                    onChange={(e) => setField("dbUsername", e.target.value)}
                    error={errors.dbUsername}
                  />
                  <Input
                    label="DB password"
                    type="password"
                    value={values.dbPassword}
                    onChange={(e) => setField("dbPassword", e.target.value)}
                    error={errors.dbPassword}
                    hint="Solo se envía al provisioner; nunca vuelve al cliente."
                  />
                </div>
                <div className="h-3" />
                <div className="grid grid-cols-2 gap-3">
                  <Input
                    label="HikariCP pool size"
                    type="number"
                    min={1}
                    max={1000}
                    value={String(values.poolSize ?? 10)}
                    onChange={(e) => setField("poolSize", Number(e.target.value))}
                    error={errors.poolSize}
                  />
                </div>
              </>
            )}
          </>
        )}
      </Form>
    </Drawer>
  );
}

/**
 * Native {@code <select>} for the dialect. We don't have a
 * Select primitive in {@code /components/ui} yet and the
 * three-option dropdown is small enough that a wrapper
 * around the platform control is fine — keeps the
 * RadioGroup pure (no children prop, no slot API).
 */
function DialectSelect({
  value,
  onChange,
  error,
}: {
  value: string;
  onChange: (v: string) => void;
  error?: string;
}) {
  const id = `dialect-${Math.random().toString(36).slice(2)}`;
  return (
    <div>
      <label
        htmlFor={id}
        className="mb-1 block text-sm font-medium text-slate-700"
      >
        Dialecto
        <span aria-hidden="true" className="ml-0.5 text-red-600">*</span>
      </label>
      <select
        id={id}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={error ? true : undefined}
        className={[
          "w-full rounded border bg-white px-3 py-2 text-sm text-slate-900",
          "outline-none transition-colors",
          error
            ? "border-red-400 focus:border-red-500 focus:ring-1 focus:ring-red-500"
            : "border-slate-300 focus:border-sky-500 focus:ring-1 focus:ring-sky-500",
        ].join(" ")}
      >
        <option value="" disabled>Selecciona…</option>
        {QUERY_DIALECTS.map((d) => (
          <option key={d} value={d}>{d}</option>
        ))}
      </select>
      {error ? (
        <p role="alert" className="mt-1 text-xs text-red-600">{error}</p>
      ) : null}
    </div>
  );
}