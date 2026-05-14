package com.relearn.auth.service;

import com.relearn.auth.dto.AssignRoleRequest;
import com.relearn.auth.dto.ChangePasswordRequest;
import com.relearn.auth.dto.UserResponse;
import com.relearn.auth.entity.User;
import com.relearn.auth.exception.ResourceNotFoundException;
import com.relearn.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for user management operations.
 * All methods return DTOs — entities are never exposed directly.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ----------------------------------------------------------------
    //  Get All Users (ADMIN only)
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    //  Get User by ID
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + id));
        return UserResponse.fromEntity(user);
    }

    // ----------------------------------------------------------------
    //  Get Current Student Profile (by email from JWT)
    // ----------------------------------------------------------------

    /**
     * Returns the profile of the currently authenticated user.
     * The email is extracted from the JWT token in the security context.
     *
     * @param email the authenticated user's email (from JWT)
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));
        return UserResponse.fromEntity(user);
    }

    // ----------------------------------------------------------------
    //  Change Password
    // ----------------------------------------------------------------

    /**
     * Changes the authenticated user's password.
     * Verifies the current password before applying the change.
     *
     * @param email   the authenticated user's email (from JWT)
     * @param request contains currentPassword and newPassword
     * @throws BadCredentialsException if currentPassword is wrong
     */
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));

        // Verify the current password matches what's stored
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Encode and save the new password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // ----------------------------------------------------------------
    //  Assign Role (ADMIN only)
    // ----------------------------------------------------------------

    @Transactional
    public UserResponse assignRole(Long id, AssignRoleRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + id));

        user.setRole(request.getRole());
        User updatedUser = userRepository.save(user);
        return UserResponse.fromEntity(updatedUser);
    }

    // ----------------------------------------------------------------
    //  Delete User (ADMIN only)
    // ----------------------------------------------------------------

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
