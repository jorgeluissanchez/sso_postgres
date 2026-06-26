package com.co.eurekatic.ssoadmin.dto;

import com.co.eurekatic.common.entity.Query;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Response shape for the query CRUD endpoints. Includes the
 * bound role ids so the admin UI can render the multi-select
 * without a follow-up round-trip — same pattern as
 * {@link EndpointResponse}.
 *
 * <p>{@code createdDate} is the DB-managed timestamp
 * (insertable=false, updatable=false on the entity). We surface
 * it for the admin UI; {@code query-service} ignores it.
 */
public record QueryResponse(
        Long id,
        String uuid,
        String query,
        String type,
        boolean publicEnd,
        boolean captcha,
        String detail,
        String action,
        String style,
        LocalDateTime createdDate,
        Set<Long> roleIds
) {
    public static QueryResponse fromEntity(Query q) {
        return new QueryResponse(
                q.getId(),
                q.getUuid(),
                q.getQuery(),
                q.getType(),
                q.isPublicEnd(),
                q.isCaptcha(),
                q.getDetail(),
                q.getAction(),
                q.getStyle(),
                q.getCreatedDate(),
                q.getRoles().stream()
                        .map(com.co.eurekatic.common.entity.Role::getId)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
    }
}
