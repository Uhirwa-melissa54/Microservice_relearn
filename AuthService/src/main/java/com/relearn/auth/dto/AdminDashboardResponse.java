package com.relearn.auth.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO for the admin dashboard.
 * Returns all data the admin needs on their home screen in a single call.
 *
 * Example response:
 * {
 *   "totalStudents": 320,
 *   "totalTeachers": 18,
 *   "totalActiveClasses": 12,
 *   "systemActivityPercentage": 74.5,
 *   "newUsersThisWeek": 5,
 *   "activeUsersThisMonth": 280,
 *   "recentActivities": [ ... ]
 * }
 */
@Getter
@Setter
@Builder
public class AdminDashboardResponse {

    // ----------------------------------------------------------------
    //  Top statistics
    // ----------------------------------------------------------------

    /** Total registered students */
    private long totalStudents;

    /** Total registered teachers */
    private long totalTeachers;

    /** Total distinct active classes (derived from student className values) */
    private long totalActiveClasses;

    /**
     * System activity percentage (0–100).
     * Calculated as: (events in last 7 days / baseline) * 100
     * Capped at 100.
     */
    private double systemActivityPercentage;

    /** New users registered in the last 7 days */
    private long newUsersThisWeek;

    /** Active (non-deactivated) users */
    private long activeUsers;

    // ----------------------------------------------------------------
    //  Recent activities feed (last 20 events)
    // ----------------------------------------------------------------

    private List<ActivityLogResponse> recentActivities;
}
