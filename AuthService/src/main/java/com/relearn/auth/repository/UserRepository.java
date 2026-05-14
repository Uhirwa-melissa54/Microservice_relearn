package com.relearn.auth.repository;

import com.relearn.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access layer for User entities.
 * Spring Data JPA auto-implements all standard CRUD operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Find a user by their email (used during login and JWT validation) */
    Optional<User> findByEmail(String email);

    /** Check if an email is already registered (used during registration) */
    boolean existsByEmail(String email);
}
