package com.co.eurekatic.ssoadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /group}. Idempotent — re-saving
 * a group with the same {@code name} returns the existing one.
 */
public record GroupRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 255) String description
) {}
