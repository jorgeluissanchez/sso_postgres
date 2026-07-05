import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { Table, type Column } from "@/components/ui/Table";
import { useToast } from "@/components/ui/Toast";
import {
  useCreateWrite,
  useDeleteWrite,
  useUpdateWrite,
  useWrites,
} from "@/hooks/useWrites";
import type {
  WriteDefinitionRequest,
  WriteDefinitionResponse,
} from "@/api/types";
import { WriteFormDrawer } from "./WriteFormDrawer";

/**
 * Admin CRUD for the WriteDefinition entity. The role
 * binding family is managed inside the edit drawer via the
 * Roles tab — see {@code WriteFormDrawer}. This page owns:
 * <ul>
 *   <li>the row table (6 columns: uuid, writeType, tableName,
 *       columns count, roles count, actions),</li>
 *   <li>the create/edit drawer state,</li>
 *   <li>the delete confirmation modal.</li>
 * </ul>
 *
 * <p>Behaviour (mirrors {@code AppsListPage}):
 * <ul>
 *   <li><b>Edit stays open.</b> After "Guardar cambios" the
 *       drawer remains open so the admin can keep toggling
 *       role bindings without re-opening.</li>
 *   <li><b>Create closes.</b> After "Crear" the drawer closes
 *       and the new row appears in the table (the
 *       {@code useWrites} query is invalidated by
 *       {@code useCreateWrite}).</li>
 *   <li><b>409 from the backend</b> (duplicate uuid) is
 *       surfaced by the {@code Form} component as a red toast
 *       with the error envelope's {@code code} + {@code message}.</li>
 * </ul>
 *
 * <p>The columns column shows the chip count + first chip as
 * a tooltip — enough to scan the row without opening the
 * drawer. The roles column shows the binding count.
 */
export function WritesListPage() {
  const writes = useWrites();
  const createWrite = useCreateWrite();
  const updateWrite = useUpdateWrite();
  const deleteWrite = useDeleteWrite();
  const toast = useToast();

  const [editing, setEditing] = useState<WriteDefinitionResponse | null>(null);
  const [creating, setCreating] = useState(false);
  const [deleting, setDeleting] = useState<WriteDefinitionResponse | null>(
    null,
  );

  async function handleSubmit(payload: WriteDefinitionRequest) {
    if (payload.id) {
      await updateWrite.mutateAsync(payload);
      toast.show("Write actualizado", "success");
      // drawer stays open on edit; admin keeps working
    } else {
      await createWrite.mutateAsync(payload);
      toast.show("Write creado", "success");
      setCreating(false);
    }
  }

  async function confirmDelete() {
    if (!deleting) return;
    await deleteWrite.mutateAsync(deleting.id);
    toast.show("Write eliminado", "success");
    setDeleting(null);
  }

  const columns: Column<WriteDefinitionResponse>[] = [
    {
      key: "uuid",
      header: "UUID",
      render: (w) => (
        <span
          className="inline-block max-w-[12rem] truncate align-middle font-mono text-xs text-slate-700"
          title={w.uuid}
        >
          {w.uuid}
        </span>
      ),
    },
    {
      key: "writeType",
      header: "Tipo",
      render: (w) => (
        <span
          className={[
            "rounded px-1.5 py-0.5 text-[10px] font-medium",
            w.writeType === "INSERT"
              ? "bg-sky-50 text-sky-700"
              : "bg-violet-50 text-violet-700",
          ].join(" ")}
        >
          {w.writeType}
        </span>
      ),
    },
    {
      key: "tableName",
      header: "Tabla",
      render: (w) => (
        <span
          className="inline-block max-w-[14rem] truncate align-middle font-mono text-xs text-slate-700"
          title={w.tableName}
        >
          {w.tableName}
        </span>
      ),
    },
    {
      key: "columns",
      header: "Columnas",
      render: (w) => {
        const list = parseColumnsForPreview(w.columns);
        const first = list[0];
        return (
          <span
            className="text-xs text-slate-700"
            title={list.join(", ")}
          >
            {list.length}
            {first ? (
              <span className="ml-1 text-slate-500">
                · {first}
                {list.length > 1 ? "…" : ""}
              </span>
            ) : null}
          </span>
        );
      },
    },
    {
      key: "roles",
      header: "Roles",
      render: (w) => (
        <span className="rounded bg-slate-100 px-1.5 py-0.5 text-[10px] text-slate-700">
          Roles · {w.roleIds.length}
        </span>
      ),
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (w) => (
        <div className="flex justify-end gap-2">
          <Button
            size="sm"
            variant="secondary"
            onClick={() => setEditing(w)}
            data-testid={`edit-${w.id}`}
          >
            Editar
          </Button>
          <Button
            size="sm"
            variant="danger"
            onClick={() => setDeleting(w)}
            data-testid={`delete-${w.id}`}
          >
            Eliminar
          </Button>
        </div>
      ),
    },
  ];

  return (
    <section>
      <header className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold text-slate-900">Writes</h1>
          <p className="mt-0.5 text-xs text-slate-500">
            Definiciones de INSERT/UPDATE consumidas por query-service a
            través de su UUID.
          </p>
        </div>
        <Button onClick={() => setCreating(true)} data-testid="new-write">
          + Nuevo write
        </Button>
      </header>

      <Table
        columns={columns}
        rows={writes.data ?? []}
        rowKey={(w) => w.id}
        loading={writes.isLoading}
        empty={
          writes.isError
            ? "No se pudo cargar la lista. ¿sso-admin está UP?"
            : "Aún no hay writes."
        }
      />

      <WriteFormDrawer
        open={creating || editing !== null}
        write={editing}
        onClose={() => {
          setCreating(false);
          setEditing(null);
        }}
        onSubmit={handleSubmit}
      />

      <Modal
        open={deleting !== null}
        onClose={() => setDeleting(null)}
        title="Eliminar write"
        description={
          deleting ? `Se eliminará "${deleting.uuid}" y todas sus vinculaciones a roles.` : ""
        }
        footer={
          <>
            <Button variant="secondary" onClick={() => setDeleting(null)}>
              Cancelar
            </Button>
            <Button
              variant="danger"
              loading={deleteWrite.isPending}
              onClick={() => void confirmDelete()}
              data-testid="write-confirm-delete"
            >
              Eliminar
            </Button>
          </>
        }
      >
        <p className="text-sm text-slate-600">
          Esta acción no se puede deshacer.
        </p>
      </Modal>
    </section>
  );
}

/**
 * Tolerant JSON parse used only for the table preview cell.
 * Returns {@code []} on anything malformed so a corrupt wire
 * value just shows "0" instead of crashing the row.
 */
function parseColumnsForPreview(s: string): string[] {
  if (!s) return [];
  try {
    const v = JSON.parse(s);
    return Array.isArray(v) ? v.map(String) : [];
  } catch {
    return [];
  }
}
