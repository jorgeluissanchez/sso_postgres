import { useMemo } from "react";
import { Drawer } from "@/components/ui/Drawer";
import { Input } from "@/components/ui/Input";
import { Form, zodFieldErrors } from "@/components/forms/Form";
import { routeFormSchema, type RouteFormValues } from "@/schemas";
import type { RouteResponse } from "@/api/types";

interface Props {
  open: boolean;
  route: RouteResponse | null;
  parentCandidates: RouteResponse[];
  onClose: () => void;
  onSubmit: (values: RouteFormValues & { id?: number }) => Promise<void>;
}

export function RouteFormDrawer({
  open,
  route,
  parentCandidates,
  onClose,
  onSubmit,
}: Props) {
  const initialValues: RouteFormValues = useMemo(
    () => ({
      name: route?.name ?? "",
      icon: route?.icon ?? "",
      path: route?.path ?? "",
      menuOrder: route?.menuOrder ?? 0,
      type: route?.type ?? "MENU",
      idParent: route?.idParent ?? null,
    }),
    [route],
  );

  return (
    <Drawer
      open={open}
      onClose={onClose}
      title={route ? `Editar ruta: ${route.name}` : "Nueva ruta"}
      description="Una ruta es una entrada de menú. Los roles que la ven se asignan en otra pantalla."
      footer={null}
    >
      <Form<RouteFormValues>
        initialValues={initialValues}
        validate={(values) => {
          const result = routeFormSchema.safeParse(values);
          if (result.success) return {};
          return zodFieldErrors(result.error);
        }}
        onSubmit={async (values) => {
          await onSubmit(route ? { id: route.id, ...values } : values);
        }}
        onCancel={onClose}
        submitLabel={route ? "Guardar cambios" : "Crear"}
      >
        {({ values, setField, errors }) => (
          <>
            <div className="grid grid-cols-[1fr_8rem] gap-3">
              <Input
                label="Nombre"
                required
                value={values.name}
                onChange={(e) => setField("name", e.target.value)}
                error={errors.name}
              />
              <Input
                label="Orden"
                type="number"
                min={0}
                value={values.menuOrder}
                onChange={(e) => setField("menuOrder", Number(e.target.value))}
                error={errors.menuOrder}
              />
            </div>
            <div className="h-3" />
            <div className="grid grid-cols-[1fr_8rem] gap-3">
              <Input
                label="Path"
                required
                value={values.path}
                onChange={(e) => setField("path", e.target.value)}
                error={errors.path}
                hint="Path interno de la SPA, ej. /admin/users"
              />
              <Input
                label="Icono"
                value={values.icon}
                onChange={(e) => setField("icon", e.target.value)}
                error={errors.icon}
                hint="Clase de icono"
              />
            </div>
            <div className="h-3" />
            <div className="grid grid-cols-2 gap-3">
              <label className="block text-sm">
                <span className="mb-1 block font-medium text-slate-700">
                  Tipo<span className="ml-0.5 text-red-600">*</span>
                </span>
                <select
                  value={values.type}
                  onChange={(e) => setField("type", e.target.value)}
                  className="w-full rounded border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-sky-500 focus:ring-1 focus:ring-sky-500"
                >
                  <option value="MENU">MENU</option>
                  <option value="ITEM">ITEM</option>
                </select>
                {errors.type ? (
                  <p role="alert" className="mt-1 text-xs text-red-600">
                    {errors.type}
                  </p>
                ) : null}
              </label>
              <label className="block text-sm">
                <span className="mb-1 block font-medium text-slate-700">Padre</span>
                <select
                  value={values.idParent ?? ""}
                  onChange={(e) => {
                    const v = e.target.value;
                    setField("idParent", v === "" ? null : Number(v));
                  }}
                  className="w-full rounded border border-slate-300 bg-white px-3 py-2 text-sm outline-none focus:border-sky-500 focus:ring-1 focus:ring-sky-500"
                >
                  <option value="">(raíz)</option>
                  {parentCandidates
                    .filter((c) => c.id !== route?.id)
                    .map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                </select>
              </label>
            </div>
          </>
        )}
      </Form>
    </Drawer>
  );
}
