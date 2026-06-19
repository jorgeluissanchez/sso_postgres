import { useMemo } from "react";
import { Drawer } from "@/components/ui/Drawer";
import { Input } from "@/components/ui/Input";
import { Form, zodFieldErrors } from "@/components/forms/Form";
import { groupFormSchema, type GroupFormValues } from "@/schemas";

interface Props {
  open: boolean;
  onClose: () => void;
  onSubmit: (values: GroupFormValues) => Promise<void>;
}

export function GroupFormDrawer({ open, onClose, onSubmit }: Props) {
  const initialValues: GroupFormValues = useMemo(
    () => ({ name: "", description: "" }),
    [],
  );

  return (
    <Drawer
      open={open}
      onClose={onClose}
      title="Nuevo grupo"
      description="Los grupos se usan para organizar usuarios."
      footer={null}
    >
      <Form<GroupFormValues>
        initialValues={initialValues}
        validate={(values) => {
          const result = groupFormSchema.safeParse(values);
          if (result.success) return {};
          return zodFieldErrors(result.error);
        }}
        onSubmit={onSubmit}
        onCancel={onClose}
        submitLabel="Crear grupo"
      >
        {({ values, setField, errors }) => (
          <>
            <Input
              label="Nombre"
              required
              value={values.name}
              onChange={(e) => setField("name", e.target.value)}
              error={errors.name}
            />
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
