package com.relearn.assignment.enums;

/**
 * Lifecycle status of a student's submission.
 *
 * PENDING   -> Submitted on time, awaiting teacher review
 * LATE      -> Submitted after the assignment deadline
 * GRADED    -> Teacher has reviewed and assigned a grade
 * MISSING   -> Assignment deadline passed, student never submitted
 */
public enum SubmissionStatus {
    PENDING,
    LATE,
    GRADED,
    MISSING
}
