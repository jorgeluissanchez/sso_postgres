package com.co.eurekatic.ssoadmin.dto;

import com.co.eurekatic.common.entity.Query;

/**
 * Read-side response shape for {@code GET /getQuery?uuid=...}.
 * Consumed by {@code query-service} via the
 * {@code CatalogClient.resolveQuery} call.
 *
 * <p>This is the wire format — we deliberately do NOT include
 * the {@code id} or the bound role set, because the consumer
 * has no business with them. The fields are an exact match to
 * the legacy {@code Map<String, Object>} returned by
 * {@code sso-service}'s {@code getOnlyQuery(...)} so the
 * downstream {@code ReadService} can deserialize the same
 * shape it always did.
 *
 * <p>JSON keys mirror the legacy uppercase columns
 * ({@code ID_QUERY}, {@code QUERY}, {@code PUBLIC_END},
 * {@code CAPTCHA}, {@code TYPE}, {@code DETAIL}, {@code ACTION},
 * {@code STYLE}). Keeping the same names keeps the JSON
 * backward-compatible with any external consumer that already
 * parses the legacy response.
 */
public record QueryDefinition(
        Long idQuery,
        String uuid,
        String query,
        String type,
        boolean publicEnd,
        boolean captcha,
        String detail,
        String action,
        String style
) {
    public static QueryDefinition fromEntity(Query q) {
        return new QueryDefinition(
                q.getId(),
                q.getUuid(),
                q.getQuery(),
                q.getType(),
                q.isPublicEnd(),
                q.isCaptcha(),
                q.getDetail(),
                q.getAction(),
                q.getStyle());
    }
}
