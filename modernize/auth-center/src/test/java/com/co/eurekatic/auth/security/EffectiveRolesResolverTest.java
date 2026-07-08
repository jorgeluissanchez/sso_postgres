package com.co.eurekatic.auth.security;

import com.co.eurekatic.common.entity.Group;
import com.co.eurekatic.common.entity.Role;
import com.co.eurekatic.common.entity.User;
import com.co.eurekatic.common.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EffectiveRolesResolverTest {

    private final UserRepository repo = mock(UserRepository.class);
    private final EffectiveRolesResolver resolver = new EffectiveRolesResolver(repo);

    private Role role(String name) { Role r = new Role(); r.setName(name); return r; }

    @Test
    void unionsDirectAndGroupRolesDeduplicated() {
        User u = new User();
        u.setUsername("alice");
        u.addRole(role("USER"));            // direct
        Group g = new Group("ops");
        g.addRole(role("QUERY_READER"));    // via group
        g.addRole(role("USER"));            // duplicate of direct
        u.getGroups().add(g);

        when(repo.findByUsernameWithEffectiveRoles("alice")).thenReturn(Optional.of(u));

        Set<String> roles = resolver.forUsername("alice");

        assertThat(roles).containsExactlyInAnyOrder("USER", "QUERY_READER");
    }

    @Test
    void unknownUserYieldsEmptySet() {
        when(repo.findByUsernameWithEffectiveRoles("ghost")).thenReturn(Optional.empty());
        assertThat(resolver.forUsername("ghost")).isEqualTo(new LinkedHashSet<String>());
    }
}
