package com.co.eurekatic.ssoadmin.service;

import com.co.eurekatic.common.entity.Role;
import com.co.eurekatic.common.entity.WriteDefinition;
import com.co.eurekatic.common.repository.RoleRepository;
import com.co.eurekatic.common.repository.WriteDefinitionRepository;
import com.co.eurekatic.ssoadmin.dto.WriteDefinitionRequest;
import com.co.eurekatic.ssoadmin.dto.WriteDefinitionResponse;
import com.co.eurekatic.ssoadmin.exception.DuplicateException;
import com.co.eurekatic.ssoadmin.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * WriteDefinition CRUD plus role bindings. Same shape as
 * {@link QueryAdminService} — see that class for the rationale
 * on uniqueness pre-check and DB-level constraint.
 *
 * <p>This service is the ADMIN surface (manage the catalog).
 * The catalog endpoint that {@code query-service} consumes is
 * a separate read-side class ({@code QueryCatalogController} /
 * {@code WriteCatalogController}) that performs the username
 * authorization check against the {@code ROLE_WRITE} join.
 */
@Service
public class WriteDefinitionAdminService {

    private final WriteDefinitionRepository writeRepo;
    private final RoleRepository roleRepo;

    public WriteDefinitionAdminService(WriteDefinitionRepository writeRepo,
                                       RoleRepository roleRepo) {
        this.writeRepo = writeRepo;
        this.roleRepo = roleRepo;
    }

    @Transactional
    public WriteDefinitionResponse create(WriteDefinitionRequest req) {
        if (writeRepo.existsByUuid(req.uuid())) {
            throw new DuplicateException("WriteDefinition", req.uuid());
        }
        WriteDefinition w = new WriteDefinition();
        copy(req, w);
        return WriteDefinitionResponse.fromEntity(writeRepo.save(w));
    }

    @Transactional
    public WriteDefinitionResponse update(WriteDefinitionRequest req) {
        if (req.id() == null) {
            throw new IllegalArgumentException("id is required for update");
        }
        WriteDefinition w = writeRepo.findById(req.id())
                .orElseThrow(() -> new NotFoundException("WriteDefinition", req.id()));
        writeRepo.findByUuid(req.uuid()).ifPresent(existing -> {
            if (!existing.getId().equals(w.getId())) {
                throw new DuplicateException("WriteDefinition", req.uuid());
            }
        });
        copy(req, w);
        return WriteDefinitionResponse.fromEntity(writeRepo.save(w));
    }

    @Transactional(readOnly = true)
    public List<WriteDefinitionResponse> getAll() {
        return writeRepo.findAll().stream()
                .map(WriteDefinitionResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public WriteDefinitionResponse getById(Long id) {
        return WriteDefinitionResponse.fromEntity(writeRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("WriteDefinition", id)));
    }

    @Transactional
    public void delete(Long id) {
        if (!writeRepo.existsById(id)) {
            throw new NotFoundException("WriteDefinition", id);
        }
        writeRepo.deleteById(id);
    }

    /* ====================== bindings: role ====================== */

    @Transactional
    public void bindRole(Long writeId, Long roleId) {
        WriteDefinition w = writeRepo.findById(writeId)
                .orElseThrow(() -> new NotFoundException("WriteDefinition", writeId));
        Role r = roleRepo.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role", roleId));
        w.addRole(r);
    }

    @Transactional
    public void unbindRole(Long writeId, Long roleId) {
        WriteDefinition w = writeRepo.findById(writeId)
                .orElseThrow(() -> new NotFoundException("WriteDefinition", writeId));
        Role r = roleRepo.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role", roleId));
        w.removeRole(r);
    }

    public record RoleChecked(Long id, String name, boolean bound) {}

    @Transactional(readOnly = true)
    public List<RoleChecked> getRolesForWriteChecked(Long writeId) {
        WriteDefinition w = writeRepo.findById(writeId)
                .orElseThrow(() -> new NotFoundException("WriteDefinition", writeId));
        Set<Long> bound = w.getRoles().stream()
                .map(Role::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return roleRepo.findAll().stream()
                .map(r -> new RoleChecked(r.getId(), r.getName(), bound.contains(r.getId())))
                .toList();
    }

    /* ====================== helpers ====================== */

    private void copy(WriteDefinitionRequest req, WriteDefinition w) {
        w.setUuid(req.uuid());
        w.setWriteType(req.writeType());
        w.setTableName(req.tableName());
        w.setColumns(req.columns());
        w.setKeyColumns(req.keyColumns());
    }
}
