package com.co.eurekatic.ssoadmin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for {@code PUT /updateAccount}. All fields
 * are optional except {@code username} (the lookup key).
 * {@code null} fields are left unchanged.
 */
public record UpdateAccountRequest(
        @NotBlank @Size(min = 3, max = 80) String username,
        @Size(max = 200) String fullName,
        @Email @Size(max = 200) String email,
        Boolean active,
        Boolean ldap,
        /** If non-null, replaces the user's role set. */
        List<@NotBlank String> roleNames
) {}
