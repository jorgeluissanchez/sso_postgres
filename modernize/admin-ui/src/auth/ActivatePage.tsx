import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { apiClient } from "@/api/client";

/**
 * Public activation page. The user clicks the link in their email
 * (or pastes the URL after registration) and lands here. We
 * forward the token to the backend; on success we show a "your
 * account is active, please log in" message.
 *
 * The endpoint is permitAll in the backend, so we call it
 * directly without a Bearer header.
 */
export function ActivatePage() {
  const [params] = useSearchParams();
  const token = params.get("token") ?? "";
  const [state, setState] = useState<"pending" | "ok" | "error">("pending");
  const [message, setMessage] = useState<string>("Activando tu cuenta…");

  useEffect(() => {
    if (!token) {
      setState("error");
      setMessage("Falta el token de activación.");
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        await apiClient.get(
          `/sso-admin/activateAccount?token=${encodeURIComponent(token)}`,
          { skipAuth: true },
        );
        if (cancelled) return;
        setState("ok");
        setMessage("Tu cuenta ha sido activada. Inicia sesión para continuar.");
      } catch (err) {
        if (cancelled) return;
        setState("error");
        setMessage(
          err instanceof Error ? err.message : "No se pudo activar la cuenta.",
        );
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [token]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-100 px-4">
      <div
        role="alert"
        className={`w-full max-w-md rounded-lg border p-6 shadow-sm ${
          state === "ok"
            ? "border-emerald-200 bg-white"
            : state === "error"
              ? "border-red-200 bg-white"
              : "border-slate-200 bg-white"
        }`}
      >
        <h1 className="mb-2 text-lg font-semibold text-slate-900">
          Activación de cuenta
        </h1>
        <p className="text-sm text-slate-700">{message}</p>
        {state === "ok" ? (
          <a
            href="/login"
            className="mt-4 inline-block rounded bg-sky-600 px-3 py-2 text-sm font-medium text-white hover:bg-sky-700"
          >
            Ir a iniciar sesión
          </a>
        ) : null}
      </div>
    </main>
  );
}
