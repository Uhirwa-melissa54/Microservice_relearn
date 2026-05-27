package com.relearn.auth.enums;

/**
 * Categorizes activity log events for filtering and display.
 */
public enum ActivityType {

    // User events
    USER_REGISTERED,
    USER_UPDATED,
    USER_DELETED,
    USER_DEACTIVATED,
    USER_REACTIVATED,
    PASSWORD_CHANGED,
    ROLE_ASSIGNED,

    // Class events
    CLASS_CREATED,
    CLASS_UPDATED,
    TEACHER_ASSIGNED_TO_CLASS,
    STUDENT_ASSIGNED_TO_CLASS,

    // Content events
    NOTE_UPLOADED,
    NOTE_UPDATED,
    NOTE_DELETED,
    ASSIGNMENT_CREATED,
    ASSIGNMENT_UPDATED,
    ASSIGNMENT_DELETED,

    // Submission events
    ASSIGNMENT_SUBMITTED,
    SUBMISSION_GRADED,

    // System events
    SYSTEM
}
