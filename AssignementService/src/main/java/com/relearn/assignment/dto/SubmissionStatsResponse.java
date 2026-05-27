package com.relearn.assignment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for submission statistics on a single assignment.
 * Used by the teacher's assignment submissions page header.
 *
 * Example:
 * {
 *   "assignmentId": 1,
 *   "assignmentTitle": "Chapter 5 Exercises",
 *   "totalExpected": 35,
 *   "totalSubmitted": 28,
 *   "totalGraded": 20,
 *   "totalPending": 8,
 *   "totalLate": 3,
 *   "totalMissing": 7
 * }
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class SubmissionStatsResponse {

    private Long assignmentId;
    private String assignmentTitle;
    private String className;
    private String courseName;

    /** Total students expected to submit (class size) */
    private long totalExpected;

    /** Students who actually submitted (any status except MISSING) */
    private long totalSubmitted;

    /** Submissions that have been graded */
    private long totalGraded;

    /** Submissions waiting for grading (PENDING + LATE) */
    private long totalPendingReview;

    /** Submissions submitted after the deadline */
    private long totalLate;

    /** Students who never submitted */
    private long totalMissing;
}
