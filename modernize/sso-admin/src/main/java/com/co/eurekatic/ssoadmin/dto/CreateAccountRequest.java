package com.co.eurekatic.ssoadmin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for {@code POST /createAccount}. Mirrors the
 * legacy {@code com.co.lowcode.sso.model.User} DTO from
 * {@code sso-service}, with modern Jakarta Bean Validation
 * annotations.
 *
 * <p>Note: the legacy accepted {@code PASSWORDCONFIRM} for
 * client-side validation. The modernized version validates the
 * match in the service layer instead — exposing it on the
 * request DTO would be useless and would leak the rule into
 * the API contract.
 */
public record CreateAccountRequest(
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Size(min = 3, max = 80) String username,
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(min = 6, max = 100) String password,
        String passwordConfirm,
        /** Roles to grant. Empty list → no roles. */
        List<@NotBlank String> roleNames
) {}
