package com.co.eurekatic.ssoadmin.service;

import com.co.eurekatic.common.entity.Query;
import com.co.eurekatic.common.entity.Role;
import com.co.eurekatic.common.repository.QueryRepository;
import com.co.eurekatic.common.repository.RoleRepository;
import com.co.eurekatic.ssoadmin.dto.QueryRequest;
import com.co.eurekatic.ssoadmin.dto.QueryResponse;
import com.co.eurekatic.ssoadmin.exception.DuplicateException;
import com.co.eurekatic.ssoadmin.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Query CRUD plus role bindings. Mirrors the shape of
 * {@link EndpointService} so the admin UI can use the same
 * patterns (list + create + update + delete + bind/unbind
 * role). The catalog endpoint on the read side (see
 * {@code QueryCatalogController}) is what {@code query-service}
 * calls — it does NOT go through this service.
 *
 * <p>Uniqueness: the legacy rule is {@code uuid}. We pre-check
 * with {@link QueryRepository#existsByUuid} for a friendlier
 * 409; the DB-level {@code UQ_QUERY_UUID} constraint is the
 * real source of truth under concurrent inserts.
 */
@Service
public class QueryAdminService {

    private final QueryRepository queryRepo;
    private final RoleRepository roleRepo;

    public QueryAdminService(QueryRepository queryRepo, RoleRepository roleRepo) {
        this.queryRepo = queryRepo;
        this.roleRepo = roleRepo;
    }

    @Transactional
    public QueryResponse create(QueryRequest req) {
        if (queryRepo.existsByUuid(req.uuid())) {
            throw new DuplicateException("Query", req.uuid());
        }
        Query q = new Query();
        copy(req, q);
        return QueryResponse.fromEntity(queryRepo.save(q));
    }

    @Transactional
    public QueryResponse update(QueryRequest req) {
        if (req.id() == null) {
            throw new IllegalArgumentException("id is required for update");
        }
        Query q = queryRepo.findById(req.id())
                .orElseThrow(() -> new NotFoundException("Query", req.id()));
        // Allow same uuid only for the same row.
        queryRepo.findByUuid(req.uuid()).ifPresent(existing -> {
            if (!existing.getId().equals(q.getId())) {
                throw new DuplicateException("Query", req.uuid());
            }
        });
        copy(req, q);
        return QueryResponse.fromEntity(queryRepo.save(q));
    }

    @Transactional(readOnly = true)
    public List<QueryResponse> getAll() {
        return queryRepo.findAll().stream()
                .map(QueryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public QueryResponse getById(Long id) {
        return QueryResponse.fromEntity(queryRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Query", id)));
    }

    @Transactional
    public void delete(Long id) {
        if (!queryRepo.existsById(id)) {
            throw new NotFoundException("Query", id);
        }
        queryRepo.deleteById(id);
    }

    /* ====================== bindings: role ====================== */

    @Transactional
    public void bindRole(Long queryId, Long roleId) {
        Query q = queryRepo.findById(queryId)
                .orElseThrow(() -> new NotFoundException("Query", queryId));
        Role r = roleRepo.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role", roleId));
        q.addRole(r);
    }

    @Transactional
    public void unbindRole(Long queryId, Long roleId) {
        Query q = queryRepo.findById(queryId)
                .orElseThrow(() -> new NotFoundException("Query", queryId));
        Role r = roleRepo.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role", roleId));
        q.removeRole(r);
    }

    /**
     * Compact DTO matching the {@code EndpointService.RoleChecked}
     * shape: id plus a flag telling the admin UI whether the
     * role is currently bound to the query.
     */
    public record RoleChecked(Long id, String name, boolean bound) {}

    @Transactional(readOnly = true)
    public List<RoleChecked> getRolesForQueryChecked(Long queryId) {
        Query q = queryRepo.findById(queryId)
                .orElseThrow(() -> new NotFoundException("Query", queryId));
        Set<Long> bound = q.getRoles().stream()
                .map(Role::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return roleRepo.findAll().stream()
                .map(r -> new RoleChecked(r.getId(), r.getName(), bound.contains(r.getId())))
                .toList();
    }

    /* ====================== helpers ====================== */

    private void copy(QueryRequest req, Query q) {
        q.setUuid(req.uuid());
        q.setQuery(req.query());
        q.setType(req.type());
        q.setPublicEnd(req.publicEnd());
        q.setCaptcha(req.captcha());
        q.setDetail(req.detail());
        q.setAction(req.action());
        q.setStyle(req.style());
    }
}
