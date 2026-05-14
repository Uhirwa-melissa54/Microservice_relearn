package com.relearn.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for creating or updating an assignment.
 *
 * Note: @Future removed from deadline intentionally.
 * When updating an existing assignment, the deadline may already be in the past
 * and we still need to allow edits to other fields (title, description, etc.).
 * Deadline validation (must be future for NEW assignments) is handled in the service layer.
 */
@Getter
@Setter
public class AssignmentRequest {

    @NotBlank(message = "Title is required")
    private String title;

    /** Optional — detailed instructions for the assignment */
    private String description;

    @NotNull(message = "Deadline is required")
    private LocalDateTime deadline;

    @NotBlank(message = "Class name is required")
    private String className;

    @NotBlank(message = "Course name is required")
    private String courseName;

    /** Optional — academic year (e.g. "2024-2025") */
    private String academicYear;

    @NotNull(message = "Teacher ID is required")
    @Positive(message = "Teacher ID must be a positive number")
    private Long teacherId;

    /** Optional — URL to an attached file students can download */
    private String fileUrl;
}
