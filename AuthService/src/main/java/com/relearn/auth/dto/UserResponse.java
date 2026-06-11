package com.relearn.auth.dto;

import com.relearn.auth.entity.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for returning user data in API responses.
 * Never exposes the password field.
 */
@Getter
@Setter
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String role;
    private String className;
    private String academicYear;
    private boolean active;
    private boolean mustChangePassword;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Only populated once — immediately after admin creates a user.
     * Contains the plain-text generated password so the admin can see it.
     * Never stored, never returned again after the creation response.
     */
    private String generatedPassword;

    public static UserResponse fromEntity(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setClassName(user.getClassName());
        response.setAcademicYear(user.getAcademicYear());
        response.setActive(user.isActive());
        response.setMustChangePassword(user.isMustChangePassword());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
