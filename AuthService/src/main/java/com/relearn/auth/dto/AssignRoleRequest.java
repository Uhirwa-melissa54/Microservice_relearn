package com.relearn.auth.dto;

import com.relearn.auth.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for assigning a new role to an existing user.
 * Only accessible by ADMIN.
 */
@Getter
@Setter
public class AssignRoleRequest {

    @NotNull(message = "Role is required (ADMIN, TEACHER, or STUDENT)")
    private Role role;
}
