package com.relearn.assignment.dto;

import com.relearn.assignment.entity.Assignment;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for an assignment with its submission statistics.
 * Used in the teacher dashboard recent assignments list
 * and the teacher's assignment list page.
 *
 * Example:
 * {
 *   "id": 1,
 *   "title": "Chapter 5 Exercises",
 *   "className": "Y1A",
 *   "courseName": "Mathematics",
 *   "deadline": "2025-06-15T23:59:00",
 *   "assignmentStatus": "ACTIVE",
 *   "submissionType": "BOTH",
 *   "totalSubmitted": 28,
 *   "totalPendingReview": 8,
 *   "totalGraded": 20
 * }
 */
@Getter
@Setter
public class AssignmentWithStatsResponse {

    private Long id;
    private String title;
    private String description;
    private String className;
    private String courseName;
    private String academicYear;
    private LocalDateTime deadline;
    private String assignmentStatus;   // ACTIVE or OVERDUE
    private String submissionType;
    private String fileUrl;
    private LocalDateTime createdAt;

    // Submission stats — populated by the service
    private long totalSubmitted;
    private long totalPendingReview;
    private long totalGraded;
    private long totalLate;

    public static AssignmentWithStatsResponse fromEntity(Assignment assignment) {
        AssignmentWithStatsResponse r = new AssignmentWithStatsResponse();
        r.setId(assignment.getId());
        r.setTitle(assignment.getTitle());
        r.setDescription(assignment.getDescription());
        r.setClassName(assignment.getClassName());
        r.setCourseName(assignment.getCourseName());
        r.setAcademicYear(assignment.getAcademicYear());
        r.setDeadline(assignment.getDeadline());
        r.setFileUrl(assignment.getFileUrl());
        r.setCreatedAt(assignment.getCreatedAt());
        r.setSubmissionType(
            assignment.getSubmissionType() != null
                ? assignment.getSubmissionType().name()
                : "BOTH"
        );
        r.setAssignmentStatus(
            LocalDateTime.now().isAfter(assignment.getDeadline()) ? "OVERDUE" : "ACTIVE"
        );
        return r;
    }
}
