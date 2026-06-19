import { z } from "zod";

/**
 * Form schemas. Zod is the single source of truth — we derive
 * TS types from it and use the same schema to validate the form
 * before submit. Backend has its own validation; the point of
 * these schemas is to fail fast in the UI, not to enforce the
 * server-side contract.
 */

export const userFormSchema = z.object({
  username: z
    .string()
    .min(3, "Mínimo 3 caracteres")
    .max(50)
    .regex(/^[a-zA-Z0-9._-]+$/, "Solo letras, números, . _ -"),
  fullName: z.string().min(1, "Requerido").max(120),
  email: z.string().email("Email inválido").max(120),
  password: z.string().min(8, "Mínimo 8 caracteres").max(72).optional(),
  roleNames: z.array(z.string()).default([]),
});

export const roleFormSchema = z.object({
  name: z.string().min(2, "Mínimo 2 caracteres").max(60),
  description: z.string().max(200).default(""),
});

export const groupFormSchema = z.object({
  name: z.string().min(2, "Mínimo 2 caracteres").max(60),
  description: z.string().max(200).default(""),
});

export const microserviceFormSchema = z.object({
  serviceId: z.string().min(2, "Mínimo 2 caracteres").max(60),
  description: z.string().max(200).default(""),
  requestUri: z.string().min(1, "Requerido").max(200),
  targetUriPath: z.string().min(1, "Requerido").max(200),
  targetUrlHost: z.string().min(1, "Requerido").max(120),
  targetUrlPort: z.string().min(1, "Requerido").max(6),
});

export const endpointFormSchema = z.object({
  method: z.enum(["GET", "POST", "PUT", "DELETE", "PATCH"], {
    errorMap: () => ({ message: "Método inválido" }),
  }),
  path: z.string().min(1, "Requerido").max(200),
  description: z.string().max(200).default(""),
  numberParams: z.coerce.number().int().min(0).max(20),
});

export const routeFormSchema = z.object({
  name: z.string().min(2, "Mínimo 2 caracteres").max(60),
  icon: z.string().max(60).default(""),
  path: z.string().min(1, "Requerido").max(200),
  menuOrder: z.coerce.number().int().min(0).max(9999).default(0),
  type: z.string().min(1).max(20),
  idParent: z.number().int().nullable().default(null),
});

export type UserFormValues = z.infer<typeof userFormSchema>;
export type RoleFormValues = z.infer<typeof roleFormSchema>;
export type GroupFormValues = z.infer<typeof groupFormSchema>;
export type MicroserviceFormValues = z.infer<typeof microserviceFormSchema>;
export type EndpointFormValues = z.infer<typeof endpointFormSchema>;
export type RouteFormValues = z.infer<typeof routeFormSchema>;
