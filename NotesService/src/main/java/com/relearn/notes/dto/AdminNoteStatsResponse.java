package com.relearn.notes.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for admin-level notes statistics.
 * Returned by GET /api/admin/notes/stats
 *
 * Used by the admin dashboard to show system-wide notes activity.
 */
@Getter
@Setter
@Builder
public class AdminNoteStatsResponse {

    /** Total notes uploaded across the entire system */
    private long totalNotes;

    /** Number of distinct classes that have notes */
    private long classesWithNotes;

    /** Number of distinct courses that have notes */
    private long coursesWithNotes;

    /** Number of distinct teachers who have uploaded notes */
    private long activeTeachers;
}
