package com.relearn.auth.controller;

import com.relearn.auth.dto.AssignRoleRequest;
import com.relearn.auth.dto.UserResponse;
import com.relearn.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for user management endpoints.
 *
 * Role-based access is enforced using @PreAuthorize annotations.
 * Spring Security must be authenticated before reaching these endpoints.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users
     *
     * Returns a list of all registered users.
     * ADMIN only.
     *
     * Requires: Authorization: Bearer <accessToken>  (ADMIN role)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * GET /api/users/{id}
     *
     * Returns a single user by ID.
     * Any authenticated user can call this (e.g., to view their own profile).
     *
     * Requires: Authorization: Bearer <accessToken>
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * PATCH /api/users/{id}/role
     *
     * Assigns a new role to a user.
     * ADMIN only.
     *
     * Requires: Authorization: Bearer <accessToken>  (ADMIN role)
     *
     * Sample request body:
     * {
     *   "role": "TEACHER"
     * }
     */
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable Long id,
            @Valid @RequestBody AssignRoleRequest request) {
        return ResponseEntity.ok(userService.assignRole(id, request));
    }

    /**
     * DELETE /api/users/{id}
     *
     * Permanently deletes a user.
     * ADMIN only.
     *
     * Requires: Authorization: Bearer <accessToken>  (ADMIN role)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
