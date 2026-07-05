package com.co.eurekatic.ssoadmin.dto;

import com.co.eurekatic.common.entity.App;
import com.co.eurekatic.common.entity.Route;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Response shape for the route CRUD endpoints. Includes the
 * bound role ids so the legacy UI can render the multi-select
 * without a follow-up round-trip. {@code idParent} is null
 * for root routes (the legacy "0" sentinel is normalized away
 * on write, never echoed back).
 */
public record RouteResponse(
        Long id,
        String name,
        String icon,
        String path,
        Integer menuOrder,
        String type,
        Long idParent,
        Long appId,
        String appName,
        Set<Long> roleIds
) {
    public static RouteResponse fromEntity(Route r) {
        App app = r.getApp();
        return new RouteResponse(
                r.getId(),
                r.getName(),
                r.getIcon(),
                r.getPath(),
                r.getMenuOrder(),
                r.getType(),
                r.getIdParent(),
                app != null ? app.getId() : null,
                app != null ? app.getName() : null,
                r.getRoles().stream()
                        .map(com.co.eurekatic.common.entity.Role::getId)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
    }
}
