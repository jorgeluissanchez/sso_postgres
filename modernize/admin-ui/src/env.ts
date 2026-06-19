import { z } from "zod";

/**
 * Runtime-validated environment. Vite replaces the values at build
 * time; Zod catches missing or malformed values at startup so we
 * fail fast with a useful error rather than discovering the
 * problem in a downstream fetch.
 *
 * VITE_API_BASE is required. The path /api in dev and prod is the
 * gateway-relative prefix; the gateway's WebFlux router forwards
 * /api/** to the appropriate downstream (auth-center for /api/auth/**,
 * sso-admin for /api/sso-admin/**, etc.).
 */
const envSchema = z.object({
  VITE_API_BASE: z.string().min(1).startsWith("/"),
});

const parsed = envSchema.safeParse(import.meta.env);
if (!parsed.success) {
  // The error message is intentionally compact — this is a developer
  // build error, not a runtime user error.
  throw new Error(
    `[admin-ui] Invalid environment: ${parsed.error.issues
      .map((i) => `${i.path.join(".")}: ${i.message}`)
      .join(", ")}`,
  );
}

export const env = parsed.data;
export type Env = z.infer<typeof envSchema>;
