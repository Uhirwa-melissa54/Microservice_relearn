package com.relearn.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClassUpsertRequest {
    @NotBlank(message = "Class name is required")
    private String className;

    @NotBlank(message = "Academic year is required")
    private String academicYear;

    @NotNull(message = "Capacity is required")
    private Integer capacity;

    private Long teacherId;
}
