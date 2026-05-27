package com.relearn.assignment.enums;

/**
 * Defines what type of submission a teacher allows for an assignment.
 *
 * FILE_ONLY  -> Student must upload a file
 * TEXT_ONLY  -> Student must write a text answer
 * BOTH       -> Student can submit text, file, or both
 */
public enum SubmissionType {
    FILE_ONLY,
    TEXT_ONLY,
    BOTH
}
