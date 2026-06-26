package com.co.eurekatic.ssoadmin.dto;

import com.co.eurekatic.common.entity.WriteDefinition;
import com.co.eurekatic.common.entity.WriteType;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Response shape for the write-definition CRUD endpoints.
 * Mirrors {@link QueryResponse}'s structure so the admin UI can
 * reuse the same table component.
 */
public record WriteDefinitionResponse(
        Long id,
        String uuid,
        WriteType writeType,
        String tableName,
        String columns,
        String keyColumns,
        LocalDateTime createdDate,
        Set<Long> roleIds
) {
    public static WriteDefinitionResponse fromEntity(WriteDefinition w) {
        return new WriteDefinitionResponse(
                w.getId(),
                w.getUuid(),
                w.getWriteType(),
                w.getTableName(),
                w.getColumns(),
                w.getKeyColumns(),
                w.getCreatedDate(),
                w.getRoles().stream()
                        .map(com.co.eurekatic.common.entity.Role::getId)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
    }
}
