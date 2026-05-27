package com.relearn.auth.service;

import com.relearn.auth.dto.*;
import com.relearn.auth.entity.User;
import com.relearn.auth.enums.ActivityType;
import com.relearn.auth.enums.Role;
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
 * Service for user profile and management operations.
 * Used by students, teachers, and admins for their own profile actions.
 * Admin bulk operations are handled by AdminService.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository     userRepository;
    private final PasswordEncoder    passwordEncoder;
    private final ActivityLogService activityLogService;

    // ----------------------------------------------------------------
    //  Generic user lookup (used by multiple roles)
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return UserResponse.fromEntity(
                userRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "User not found with id: " + id)));
    }

    @Transactional
    public UserResponse assignRole(Long id, AssignRoleRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + id));
        user.setRole(request.getRole());
        return UserResponse.fromEntity(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    // ----------------------------------------------------------------
    //  Student / Teacher self-service profile
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(String email) {
        return UserResponse.fromEntity(
                userRepository.findByEmail(email)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "User not found with email: " + email)));
    }

    /**
     * Changes the authenticated user's own password.
     * Requires the current password for verification.
     * Logs PASSWORD_CHANGED activity.
     */
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Log the password change
        activityLogService.log(
                ActivityType.PASSWORD_CHANGED,
                String.format("%s changed their password", user.getFullName()),
                user.getId(), user.getFullName(), user.getRole().name(),
                user.getId(), "USER", null
        );
    }

    // ----------------------------------------------------------------
    //  Teacher profile
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public TeacherProfileResponse getTeacherProfile(Long teacherId) {
        User user = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher not found with id: " + teacherId));
        return TeacherProfileResponse.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public TeacherProfileResponse getTeacherProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher not found with email: " + email));
        return TeacherProfileResponse.fromEntity(user);
    }

    // ----------------------------------------------------------------
    //  Student count queries (used by teacher dashboard)
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public StudentCountResponse getStudentCountByClass(String className) {
        long count = userRepository.countByClassNameAndRole(className, Role.STUDENT);
        return new StudentCountResponse(className, count);
    }

    @Transactional(readOnly = true)
    public long getTotalStudentCount() {
        return userRepository.countByRole(Role.STUDENT);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getStudentsByClass(String className) {
        return userRepository.findByClassNameAndRole(className, Role.STUDENT)
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
