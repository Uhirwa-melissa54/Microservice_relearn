package com.relearn.notes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for creating or updating a note.
 *
 * This is what the client sends in the request body.
 * Validated before reaching the service layer.
 */
@Getter
@Setter
public class NoteRequest {

    @NotBlank(message = "Title is required")
    private String title;

    /** Optional — can be null or empty */
    private String description;

    /** Optional — URL to the uploaded file */
    private String fileUrl;

    @NotBlank(message = "Class name is required")
    private String className;

    @NotBlank(message = "Course name is required")
    private String courseName;

    @NotBlank(message = "Academic year is required (e.g. 2024-2025)")
    private String academicYear;

    @NotNull(message = "Teacher ID is required")
    @Positive(message = "Teacher ID must be a positive number")
    private Long teacherId;
}
