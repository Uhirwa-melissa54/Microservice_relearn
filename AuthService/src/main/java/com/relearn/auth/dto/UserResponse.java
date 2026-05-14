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
    private LocalDateTime createdAt;

    /**
     * Static factory method — converts a User entity to a UserResponse DTO.
     * Keeps mapping logic in one place.
     */
    public static UserResponse fromEntity(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setClassName(user.getClassName());
        response.setAcademicYear(user.getAcademicYear());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
