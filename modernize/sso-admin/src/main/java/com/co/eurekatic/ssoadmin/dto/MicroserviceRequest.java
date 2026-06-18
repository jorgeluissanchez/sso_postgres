package com.co.eurekatic.ssoadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /microservice/save} and
 * {@code PUT /microservice/update} (legacy used the same DTO
 * with an optional {@code id} for the update variant). The
 * create variant leaves {@code id} null and the server assigns
 * it.
 */
public record MicroserviceRequest(
        Long id,
        @NotBlank @Size(max = 200) String serviceId,
        @Size(max = 500) String description,
        @Size(max = 500) String requestUri,
        @Size(max = 500) String targetUriPath,
        @Size(max = 500) String targetUrlHost,
        @Size(max = 10)  String targetUrlPort
) {}
