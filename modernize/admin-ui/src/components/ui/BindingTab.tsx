import type { ReactNode } from "react";
import { Button } from "@/components/ui/Button";

/**
 * Generic checked-list renderer used by every binding tab
 * across the admin UI (App × 4 families + Write × 1 family).
 *
 * <p>Lives in {@code components/ui} (not {@code pages/*})
 * because the same shape is shared by Apps (4 binding tab
 * variants) and Writes (1 variant). Lift it out so future
 * entities with M:N bindings can reuse it without copy-paste.
 *
 * <p>The differences between binding families are parameterized:
 * <ul>
 *   <li>{@link getRowId} — each row has its id under a different
 *       field name (roleId / userId / routeId / microserviceId).
 *       Accepting a getter keeps this generic over the row
 *       type without forcing a common interface.</li>
 *   <li>{@link getRowChecked} — same idea; pulls the bound
 *       boolean out of the row.</li>
 *   <li>{@link renderRow} — the label the user sees per row
 *       (just {@code r.name} for roles, or
 *       {@code name + path} for routes, etc.).</li>
 *   <li>{@link onToggle} — POST vs DELETE depending on the
 *       current {@code checked} flag. The page knows the URL
 *       shape; this component just plumbs the click.</li>
 *   <li>{@link listTestIdPrefix} — defaults to {@code "bindings"}
 *       so legacy test code that hardcoded
 *       {@code app-bindings-{id}} keeps working when callers
 *       pass {@code "app-bindings"}. The Writes drawer passes
 *       {@code "write-bindings"}.</li>
 * </ul>
 *
 * <p>The prop is named {@code entityId} (not {@code appId}) so
 * the type doesn't lie about the use site — Writers pass a
 * write id, Microservices (potential future) would pass a
 * microservice id, etc.
 */
export interface BindingTabProps<TRow> {
  entityId: number;
  data: TRow[] | undefined;
  isLoading: boolean;
  isPending: boolean;
  emptyText: string;
  /** Prefix on the per-row toggle button testid
   *  (e.g. {@code "role-toggle"} → {@code role-toggle-10}). */
  toggleIdPrefix: string;
  /** Prefix on the wrapping {@code <ul>} testid (default
   *  {@code "bindings"}). The full id is
   *  {@code `${listTestIdPrefix}-${entityId}`}. */
  listTestIdPrefix?: string;
  getRowId: (row: TRow) => number;
  getRowChecked: (row: TRow) => boolean;
  onToggle: (rowId: number, currentlyBound: boolean) => void;
  renderRow: (row: TRow) => ReactNode;
}

export function BindingTab<TRow>({
  entityId,
  data,
  isLoading,
  isPending,
  emptyText,
  toggleIdPrefix,
  listTestIdPrefix = "bindings",
  getRowId,
  getRowChecked,
  onToggle,
  renderRow,
}: BindingTabProps<TRow>) {
  return (
    <ul
      className="divide-y divide-slate-200"
      data-testid={`${listTestIdPrefix}-${entityId}`}
    >
      {(data ?? []).map((row) => {
        const rowId = getRowId(row);
        const checked = getRowChecked(row);
        return (
          <li
            key={rowId}
            className="flex items-center justify-between py-2 text-sm"
          >
            <span className="font-medium text-slate-800">{renderRow(row)}</span>
            <Button
              size="sm"
              variant={checked ? "secondary" : "primary"}
              disabled={isPending}
              loading={isPending}
              onClick={() => onToggle(rowId, checked)}
              data-testid={`${toggleIdPrefix}-${rowId}`}
            >
              {checked ? "Desvincular" : "Vincular"}
            </Button>
          </li>
        );
      })}
      {!isLoading && data && data.length === 0 ? (
        <li className="py-2 text-sm text-slate-500">{emptyText}</li>
      ) : null}
    </ul>
  );
}
