import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { Table, type Column } from "@/components/ui/Table";
import { useToast } from "@/components/ui/Toast";
import {
  useCreateMicroservice,
  useDeleteMicroservice,
  useMicroservices,
  useUpdateMicroservice,
} from "@/hooks/useMicroservices";
import type { MicroserviceResponse } from "@/api/types";
import { MicroserviceFormDrawer } from "./MicroserviceFormDrawer";
import type { MicroserviceFormValues } from "@/schemas";

export function MicroservicesListPage() {
  const microservices = useMicroservices();
  const createMs = useCreateMicroservice();
  const updateMs = useUpdateMicroservice();
  const deleteMs = useDeleteMicroservice();
  const toast = useToast();
  const [editing, setEditing] = useState<MicroserviceResponse | null>(null);
  const [creating, setCreating] = useState(false);
  const [deleting, setDeleting] = useState<MicroserviceResponse | null>(null);

  async function handleSubmit(values: MicroserviceFormValues & { id?: number }) {
    if (values.id) {
      await updateMs.mutateAsync({
        id: values.id,
        serviceId: values.serviceId,
        description: values.description,
        requestUri: values.requestUri,
        targetUriPath: values.targetUriPath,
        targetUrlHost: values.targetUrlHost,
        targetUrlPort: values.targetUrlPort,
      });
      toast.show("Microservicio actualizado", "success");
    } else {
      await createMs.mutateAsync({
        serviceId: values.serviceId,
        description: values.description,
        requestUri: values.requestUri,
        targetUriPath: values.targetUriPath,
        targetUrlHost: values.targetUrlHost,
        targetUrlPort: values.targetUrlPort,
      });
      toast.show("Microservicio creado", "success");
    }
    setEditing(null);
    setCreating(false);
  }

  async function confirmDelete() {
    if (!deleting) return;
    await deleteMs.mutateAsync(deleting.id);
    toast.show("Microservicio eliminado", "success");
    setDeleting(null);
  }

  const columns: Column<MicroserviceResponse>[] = [
    { key: "serviceId", header: "Service ID", render: (m) => m.serviceId },
    { key: "requestUri", header: "Request URI", render: (m) => m.requestUri },
    {
      key: "target",
      header: "Target",
      render: (m) =>
        `${m.targetUrlHost}:${m.targetUrlPort}${m.targetUriPath}`,
    },
    { key: "description", header: "Descripción", render: (m) => m.description || "—" },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (m) => (
        <div className="flex justify-end gap-2">
          <Button size="sm" variant="secondary" onClick={() => setEditing(m)}>
            Editar
          </Button>
          <Button size="sm" variant="danger" onClick={() => setDeleting(m)}>
            Eliminar
          </Button>
        </div>
      ),
    },
  ];

  return (
    <section>
      <header className="mb-4 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">Microservicios</h1>
        <Button onClick={() => setCreating(true)}>+ Nuevo microservicio</Button>
      </header>
      <Table
        columns={columns}
        rows={microservices.data ?? []}
        rowKey={(m) => m.id}
        loading={microservices.isLoading}
        empty="Aún no hay microservicios."
      />
      <MicroserviceFormDrawer
        open={creating || editing !== null}
        microservice={editing}
        onClose={() => {
          setCreating(false);
          setEditing(null);
        }}
        onSubmit={handleSubmit}
      />
      <Modal
        open={deleting !== null}
        onClose={() => setDeleting(null)}
        title="Eliminar microservicio"
        description={`¿Seguro que quieres eliminar "${deleting?.serviceId}"? Esta acción no se puede deshacer.`}
        footer={
          <>
            <Button variant="secondary" onClick={() => setDeleting(null)}>
              Cancelar
            </Button>
            <Button
              variant="danger"
              loading={deleteMs.isPending}
              onClick={() => void confirmDelete()}
            >
              Eliminar
            </Button>
          </>
        }
      >
        <p className="text-sm text-slate-600">
          Si hay endpoints vinculados, el backend podría rechazar la operación.
        </p>
      </Modal>
    </section>
  );
}
