package com.co.eurekatic.ssoadmin.dto;

import com.co.eurekatic.common.entity.User;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Response shape for {@code GET /getUsers} and the create /
 * update endpoints. Never exposes the password hash.
 */
public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        boolean active,
        boolean ldap,
        Set<String> roleNames
) {
    public static UserResponse fromEntity(User u) {
        Set<String> roles = u.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getFullName(),
                u.getEmail(),
                u.isActive(),
                u.isLdap(),
                roles);
    }

    public static List<UserResponse> fromEntities(List<User> users) {
        return users.stream().map(UserResponse::fromEntity).toList();
    }
}
