package com.relearn.auth.dto;

import com.relearn.auth.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for admin creating a new user.
 *
 * Password is OPTIONAL — if not supplied the system auto-generates one.
 * The generated password is returned in the response AND emailed to the user.
 */
@Getter
@Setter
public class AdminCreateUserRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    /**
     * Optional. If null or blank, the system generates a secure random password.
     * The plain-text password is sent to the user via email.
     */
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    /** Required for STUDENT role (e.g. "Y1A") */
    private String className;

    /** Required for STUDENT role (e.g. "2024-2025") */
    private String academicYear;
}
