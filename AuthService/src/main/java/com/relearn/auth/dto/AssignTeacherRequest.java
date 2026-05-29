package com.relearn.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignTeacherRequest {
    @NotNull(message = "teacherId is required")
    private Long teacherId;
}
