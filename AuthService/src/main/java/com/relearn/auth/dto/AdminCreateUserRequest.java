package com.relearn.auth.dto;

import com.relearn.auth.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for admin creating a new user.
 * More flexible than the public RegisterRequest —
 * admin can set all fields including role, class, and academic year.
 */
@Getter
@Setter
public class AdminCreateUserRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    /**
     * Required for STUDENT role.
     * The class the student belongs to (e.g. "Y1A").
     */
    private String className;

    /**
     * Required for STUDENT role.
     * The academic year (e.g. "2024-2025").
     */
    private String academicYear;
}
