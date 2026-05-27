package com.relearn.assignment.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO for the teacher dashboard.
 * Aggregates all data the teacher needs on their home screen.
 *
 * Architecture note: This is assembled in the Assignment Service
 * because it owns assignments and submissions. Notes count is
 * passed in from the Notes Service (or provided by the teacher
 * via a separate call until an API Gateway aggregates them).
 */
@Getter
@Setter
@Builder
public class TeacherDashboardResponse {

    // ----------------------------------------------------------------
    //  Top statistics
    // ----------------------------------------------------------------

    /** Number of distinct class+course combinations this teacher teaches */
    private long totalClassAssignments;

    /** Total assignments created by this teacher */
    private long totalAssignmentsGiven;

    /** Total pending reviews (submissions not yet graded) */
    private long totalPendingReviews;

    // ----------------------------------------------------------------
    //  Class overview cards
    // ----------------------------------------------------------------

    /** One card per class+course combination the teacher teaches */
    private List<TeacherClassCardResponse> classCards;

    // ----------------------------------------------------------------
    //  Recent assignments (last 5)
    // ----------------------------------------------------------------

    private List<AssignmentWithStatsResponse> recentAssignments;
}
