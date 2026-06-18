package com.co.eurekatic.ssoadmin.controller;

import com.co.eurekatic.ssoadmin.dto.MicroserviceRequest;
import com.co.eurekatic.ssoadmin.dto.MicroserviceResponse;
import com.co.eurekatic.ssoadmin.service.MicroserviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Microservice CRUD. Path layout mirrors the legacy
 * {@code com.co.lowcode.sso.controller.MicroserviceController}
 * — endpoints are mounted under {@code /microservice} so the
 * gateway route {@code /sso-admin/microservice/**} lines up
 * with the legacy URLs.
 */
@RestController
@RequestMapping("/microservice")
public class MicroserviceController {

    private final MicroserviceService service;

    public MicroserviceController(MicroserviceService service) {
        this.service = service;
    }

    @PostMapping("/save")
    public ResponseEntity<MicroserviceResponse> create(@Valid @RequestBody MicroserviceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/update")
    public MicroserviceResponse update(@Valid @RequestBody MicroserviceRequest req) {
        return service.update(req);
    }

    @GetMapping("/getMicroservices")
    public List<MicroserviceResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/getMicroservice")
    public MicroserviceResponse getByServiceId(@RequestParam String serviceId) {
        return service.getByServiceId(serviceId);
    }

    @GetMapping("/{id}")
    public MicroserviceResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
