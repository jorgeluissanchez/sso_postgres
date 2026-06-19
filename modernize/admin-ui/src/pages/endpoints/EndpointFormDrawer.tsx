import { useMemo } from "react";
import { Drawer } from "@/components/ui/Drawer";
import { Input } from "@/components/ui/Input";
import { Form, zodFieldErrors } from "@/components/forms/Form";
import { endpointFormSchema, type EndpointFormValues } from "@/schemas";
import type { EndpointResponse } from "@/api/types";

interface Props {
  open: boolean;
  endpoint: EndpointResponse | null;
  onClose: () => void;
  onSubmit: (values: EndpointFormValues & { id?: number }) => Promise<void>;
}

const METHODS = ["GET", "POST", "PUT", "DELETE", "PATCH"] as const;

export function EndpointFormDrawer({ open, endpoint, onClose, onSubmit }: Props) {
  const initialValues: EndpointFormValues = useMemo(
    () => ({
      method: (endpoint?.method as EndpointFormValues["method"]) ?? "GET",
      path: endpoint?.path ?? "",
      description: endpoint?.description ?? "",
      numberParams: endpoint?.numberParams ?? 0,
    }),
    [endpoint],
  );

  return (
    <Drawer
      open={open}
      onClose={onClose}
      title={endpoint ? `Editar endpoint: ${endpoint.method} ${endpoint.path}` : "Nuevo endpoint"}
      description="Define un método y patrón de URI. Las vinculaciones a microservicios y roles se gestionan en otras pantallas."
      footer={null}
    >
      <Form<EndpointFormValues>
        initialValues={initialValues}
        validate={(values) => {
          const result = endpointFormSchema.safeParse(values);
          if (result.success) return {};
          return zodFieldErrors(result.error);
        }}
        onSubmit={async (values) => {
          await onSubmit(endpoint ? { id: endpoint.id, ...values } : values);
        }}
        onCancel={onClose}
        submitLabel={endpoint ? "Guardar cambios" : "Crear"}
      >
        {({ values, setField, errors }) => (
          <>
            <div className="grid grid-cols-[7rem_1fr_6rem] gap-3">
              <label className="block text-sm">
                <span className="mb-1 block font-medium text-slate-700">
                  Método<span className="ml-0.5 text-red-600">*</span>
                </span>
                <select
                  value={values.method}
                  onChange={(e) =>
                    setField("method", e.target.value as EndpointFormValues["method"])
                  }
                  className="w-full rounded border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-sky-500 focus:ring-1 focus:ring-sky-500"
                >
                  {METHODS.map((m) => (
                    <option key={m} value={m}>
                      {m}
                    </option>
                  ))}
                </select>
                {errors.method ? (
                  <p role="alert" className="mt-1 text-xs text-red-600">
                    {errors.method}
                  </p>
                ) : null}
              </label>
              <Input
                label="Path"
                required
                value={values.path}
                onChange={(e) => setField("path", e.target.value)}
                error={errors.path}
                hint="Soporta * y ** como wildcards"
              />
              <Input
                label="# params"
                required
                type="number"
                min={0}
                max={20}
                value={values.numberParams}
                onChange={(e) => setField("numberParams", Number(e.target.value))}
                error={errors.numberParams}
              />
            </div>
            <div className="h-3" />
            <Input
              label="Descripción"
              value={values.description}
              onChange={(e) => setField("description", e.target.value)}
              error={errors.description}
            />
          </>
        )}
      </Form>
    </Drawer>
  );
}
