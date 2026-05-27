package com.relearn.assignment.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for a single class card on the teacher dashboard.
 * Represents one class+course teaching assignment.
 *
 * Example:
 * {
 *   "className": "Y1B",
 *   "courseName": "Mathematics",
 *   "totalAssignments": 12,
 *   "totalPendingReviews": 5,
 *   "activeAssignments": 3,
 *   "overdueAssignments": 2
 * }
 */
@Getter
@Setter
@Builder
public class TeacherClassCardResponse {

    private String className;
    private String courseName;

    /** Total assignments created by this teacher for this class+course */
    private long totalAssignments;

    /** Submissions waiting for grading in this class+course */
    private long totalPendingReviews;

    /** Assignments whose deadline hasn't passed yet */
    private long activeAssignments;

    /** Assignments whose deadline has passed */
    private long overdueAssignments;
}
