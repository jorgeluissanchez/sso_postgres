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

/**
 * Discriminator for the two microservice flavours.
 * {@code REST} is the legacy routing-only row;
 * {@code QUERY} is a request to spin up a fresh
 * query-service container via the provisioner sidecar.
 */
export type MicroserviceKind = "REST" | "QUERY";

/**
 * When kind=QUERY these fields must be present. The cross-
 * field validation lives in the {@code .superRefine} below
 * because Zod's per-field rules can't model an "if/then"
 * cleanly. The backend also enforces this in
 * {@code MicroserviceService.create}.
 */
export const QUERY_DIALECTS = ["postgres", "oracle", "sqlserver"] as const;
export type QueryDialect = (typeof QUERY_DIALECTS)[number];

export const microserviceFormSchema = z
  .object({
    serviceId: z.string().min(2, "Mínimo 2 caracteres").max(60),
    description: z.string().max(200).default(""),
    requestUri: z.string().min(1, "Requerido").max(200),
    /* The host/port/path triumvirate is required for kind=REST
       and ignored for kind=QUERY. The cross-field "REST
       needs all three" check is enforced by the drawer's
       payload stripping and the backend's own validator —
       the schema stays permissive so a QUERY row with empty
       host/port/path validates cleanly. */
    targetUriPath: z.string().max(200).default(""),
    targetUrlHost: z.string().max(120).default(""),
    targetUrlPort: z.string().max(6).default(""),

    /* ====================== provisioning (QUERY kind) ====================== */
    kind: z.enum(["REST", "QUERY"]).default("REST"),
    dialect: z.string().default(""),
    jdbcUrl: z.string().default(""),
    dbUsername: z.string().default(""),
    dbPassword: z.string().default(""),
    poolSize: z.coerce.number().int().min(1).max(1000).default(10),
    instanceName: z.string().default(""),
  })
  .superRefine((v, ctx) => {
    if (v.kind !== "QUERY") return;
    if (!v.dialect) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["dialect"], message: "Requerido" });
    } else if (!QUERY_DIALECTS.includes(v.dialect as QueryDialect)) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["dialect"],
        message: `Debe ser uno de: ${QUERY_DIALECTS.join(", ")}`,
      });
    }
    if (!v.jdbcUrl) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["jdbcUrl"], message: "Requerido" });
    }
    if (!v.dbUsername) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["dbUsername"], message: "Requerido" });
    }
    if (!v.instanceName) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["instanceName"], message: "Requerido" });
    }
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
