package com.relearn.auth.dto;

import com.relearn.auth.entity.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for the teacher profile page.
 * Returns teacher info without exposing the password.
 */
@Getter
@Setter
public class TeacherProfileResponse {

    private Long id;
    private String fullName;
    private String email;
    private String role;
    private LocalDateTime joinedDate;

    public static TeacherProfileResponse fromEntity(User user) {
        TeacherProfileResponse r = new TeacherProfileResponse();
        r.setId(user.getId());
        r.setFullName(user.getFullName());
        r.setEmail(user.getEmail());
        r.setRole(user.getRole().name());
        r.setJoinedDate(user.getCreatedAt());
        return r;
    }
}
