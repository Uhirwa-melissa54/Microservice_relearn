package com.relearn.assignment.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for admin-level assignment statistics.
 * Returned by GET /api/admin/assignments/stats
 *
 * Used by the admin dashboard to show system-wide assignment activity.
 */
@Getter
@Setter
@Builder
public class AdminAssignmentStatsResponse {

    /** Total assignments ever created */
    private long totalAssignments;

    /** Assignments whose deadline is in the future */
    private long activeAssignments;

    /** Assignments whose deadline has passed */
    private long overdueAssignments;

    /** Total submissions across all assignments */
    private long totalSubmissions;

    /** Submissions waiting for grading (PENDING + LATE) */
    private long pendingReviews;

    /** Submissions that have been graded */
    private long gradedSubmissions;

    /** Submissions submitted after the deadline */
    private long lateSubmissions;
}
