package com.co.eurekatic.ssoadmin.service;

import com.co.eurekatic.common.entity.Microservice;
import com.co.eurekatic.common.repository.MicroserviceRepository;
import com.co.eurekatic.ssoadmin.dto.MicroserviceRequest;
import com.co.eurekatic.ssoadmin.dto.MicroserviceResponse;
import com.co.eurekatic.ssoadmin.exception.DuplicateException;
import com.co.eurekatic.ssoadmin.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Microservice CRUD. The legacy
 * {@code com.co.lowcode.sso.service.MicroserviceService} also
 * exposed an {@code endpoint/checked} listing and a binding
 * helper; those concerns live on {@link EndpointService} in
 * the modern port (the join table is owned by Endpoint).
 */
@Service
public class MicroserviceService {

    private final MicroserviceRepository repo;

    public MicroserviceService(MicroserviceRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public MicroserviceResponse create(MicroserviceRequest req) {
        if (repo.existsByServiceId(req.serviceId())) {
            throw new DuplicateException("Microservice", req.serviceId());
        }
        Microservice m = new Microservice();
        copy(req, m);
        return MicroserviceResponse.fromEntity(repo.save(m));
    }

    @Transactional
    public MicroserviceResponse update(MicroserviceRequest req) {
        if (req.id() == null) {
            throw new IllegalArgumentException("id is required for update");
        }
        Microservice m = repo.findById(req.id())
                .orElseThrow(() -> new NotFoundException("Microservice", req.id()));
        copy(req, m);
        return MicroserviceResponse.fromEntity(repo.save(m));
    }

    @Transactional(readOnly = true)
    public List<MicroserviceResponse> getAll() {
        return repo.findAll().stream()
                .map(MicroserviceResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public MicroserviceResponse getById(Long id) {
        Microservice m = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Microservice", id));
        return MicroserviceResponse.fromEntity(m);
    }

    @Transactional(readOnly = true)
    public MicroserviceResponse getByServiceId(String serviceId) {
        Microservice m = repo.findByServiceId(serviceId)
                .orElseThrow(() -> new NotFoundException("Microservice", serviceId));
        return MicroserviceResponse.fromEntity(m);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Microservice", id);
        }
        repo.deleteById(id);
    }

    /* ------------- internals ------------- */

    private static void copy(MicroserviceRequest req, Microservice m) {
        m.setServiceId(req.serviceId());
        m.setDescription(req.description());
        m.setRequestUri(req.requestUri());
        m.setTargetUriPath(req.targetUriPath());
        m.setTargetUrlHost(req.targetUrlHost());
        m.setTargetUrlPort(req.targetUrlPort());
    }
}
