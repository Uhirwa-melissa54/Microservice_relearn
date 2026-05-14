package com.relearn.assignment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for creating a new submission.
 * This is what the student sends when submitting an assignment.
 *
 * At least one of submissionText or fileUrl should be provided,
 * but both are optional individually to support text-only or file-only submissions.
 */
@Getter
@Setter
public class SubmissionRequest {

    @NotNull(message = "Assignment ID is required")
    @Positive(message = "Assignment ID must be a positive number")
    private Long assignmentId;

    @NotNull(message = "Student ID is required")
    @Positive(message = "Student ID must be a positive number")
    private Long studentId;

    /** The student's written answer — optional if a file is provided */
    private String submissionText;

    /** URL to the submitted file — optional if text is provided */
    private String fileUrl;
}
