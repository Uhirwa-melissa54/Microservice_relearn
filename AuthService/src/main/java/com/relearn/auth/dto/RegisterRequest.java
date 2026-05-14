package com.relearn.auth.dto;

import com.relearn.auth.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for user registration requests.
 * Validated before reaching the service layer.
 */
@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotNull(message = "Role is required (ADMIN, TEACHER, or STUDENT)")
    private Role role;

    /**
     * Class name — required when role is STUDENT (e.g. "Y1A", "Y2C").
     * Optional for TEACHER/ADMIN.
     */
    private String className;

    /**
     * Academic year — required when role is STUDENT (e.g. "2024-2025").
     * Optional for TEACHER/ADMIN.
     */
    private String academicYear;
}
