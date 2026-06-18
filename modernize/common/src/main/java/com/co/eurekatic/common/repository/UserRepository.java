package com.co.eurekatic.common.repository;

import com.co.eurekatic.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User}.
 *
 * <p>Methods are derived from the method names; no JPQL / native queries.
 * Add explicit {@code @Query} methods when the derived name would be
 * ambiguous or when you need to join across the {@code role_users} table.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByApiToken(String apiToken);

    Optional<User> findByTokenActivation(String tokenActivation);

    Optional<User> findByTokenRestore(String tokenRestore);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
