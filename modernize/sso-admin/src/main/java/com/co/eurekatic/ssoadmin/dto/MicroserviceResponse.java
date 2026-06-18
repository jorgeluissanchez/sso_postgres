package com.co.eurekatic.ssoadmin.dto;

import com.co.eurekatic.common.entity.Microservice;

/**
 * Response shape for the microservice CRUD endpoints. The
 * legacy returned a richer bag (including a list of bound
 * endpoint ids in some calls); we keep it simple and add the
 * endpoint-list getter on a separate, dedicated endpoint.
 */
public record MicroserviceResponse(
        Long id,
        String serviceId,
        String description,
        String requestUri,
        String targetUriPath,
        String targetUrlHost,
        String targetUrlPort,
        String createdDate
) {
    public static MicroserviceResponse fromEntity(Microservice m) {
        return new MicroserviceResponse(
                m.getId(),
                m.getServiceId(),
                m.getDescription(),
                m.getRequestUri(),
                m.getTargetUriPath(),
                m.getTargetUrlHost(),
                m.getTargetUrlPort(),
                m.getCreatedDate());
    }
}
