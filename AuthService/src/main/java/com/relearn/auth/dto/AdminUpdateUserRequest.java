package com.relearn.auth.dto;

import com.relearn.auth.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for admin updating an existing user.
 * All fields are required — this is a full replacement (PUT semantics).
 */
@Getter
@Setter
public class AdminUpdateUserRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotNull(message = "Role is required")
    private Role role;

    /** Class assignment — required for STUDENT role */
    private String className;

    /** Academic year — required for STUDENT role */
    private String academicYear;

    /** Whether the account is active */
    @NotNull(message = "Active status is required")
    private Boolean active;
}
