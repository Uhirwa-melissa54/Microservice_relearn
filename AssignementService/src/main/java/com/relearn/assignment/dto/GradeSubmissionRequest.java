package com.relearn.assignment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for grading a student's submission.
 * Sent by the teacher when reviewing a submission.
 */
@Getter
@Setter
public class GradeSubmissionRequest {

    @NotNull(message = "Score is required")
    @DecimalMin(value = "0.0", message = "Score cannot be negative")
    private Double score;

    @NotNull(message = "Max score is required")
    @Positive(message = "Max score must be positive")
    private Double maxScore;

    /** Optional written feedback for the student */
    private String feedback;

    @NotNull(message = "Teacher ID is required")
    @Positive(message = "Teacher ID must be positive")
    private Long gradedBy;
}
