package com.relearn.assignment.enums;

/**
 * Represents the lifecycle status of a student's submission.
 *
 * PENDING   -> Submitted on time, waiting for teacher review
 * COMPLETED -> Teacher has reviewed/graded the submission
 * LATE      -> Submitted after the assignment deadline
 */
public enum SubmissionStatus {
    PENDING,
    COMPLETED,
    LATE
}
