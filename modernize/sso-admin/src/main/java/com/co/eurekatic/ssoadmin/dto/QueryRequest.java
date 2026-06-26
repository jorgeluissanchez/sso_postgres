package com.co.eurekatic.ssoadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /query/save} and
 * {@code PUT /query/update}. The {@code uuid} is the public
 * handle clients send to {@code query-service}; uniqueness on
 * it is enforced at the DB level and pre-checked in the
 * service layer for a friendlier 409.
 *
 * <p>{@code detail}, {@code action}, and {@code style} are
 * passed through to the consumer as opaque JSON strings — the
 * admin UI is the only thing that knows their schema. The
 * catalog endpoint returns them verbatim, so whatever shape the
 * admin UI stores is what the low-code renderer sees.
 */
public record QueryRequest(
        Long id,
        @NotBlank @Size(max = 64)   String uuid,
        @NotBlank                   String query,
        @Size(max = 64)             String type,
        boolean                     publicEnd,
        boolean                     captcha,
        String                      detail,
        String                      action,
        String                      style
) {}
