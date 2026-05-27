package com.relearn.assignment.dto;

import com.relearn.assignment.entity.Assignment;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for returning assignment data in API responses.
 *
 * Includes a computed "assignmentStatus" field so the frontend
 * knows whether the assignment is ACTIVE or OVERDUE without
 * needing to compare dates client-side.
 */
@Getter
@Setter
public class AssignmentResponse {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private String className;
    private String courseName;
    private String academicYear;
    private Long teacherId;
    private String fileUrl;
    private String submissionType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String assignmentStatus;

    public static AssignmentResponse fromEntity(Assignment assignment) {
        AssignmentResponse response = new AssignmentResponse();
        response.setId(assignment.getId());
        response.setTitle(assignment.getTitle());
        response.setDescription(assignment.getDescription());
        response.setDeadline(assignment.getDeadline());
        response.setClassName(assignment.getClassName());
        response.setCourseName(assignment.getCourseName());
        response.setAcademicYear(assignment.getAcademicYear());
        response.setTeacherId(assignment.getTeacherId());
        response.setFileUrl(assignment.getFileUrl());
        response.setSubmissionType(
            assignment.getSubmissionType() != null
                ? assignment.getSubmissionType().name()
                : "BOTH"
        );
        response.setCreatedAt(assignment.getCreatedAt());
        response.setUpdatedAt(assignment.getUpdatedAt());
        response.setAssignmentStatus(
            LocalDateTime.now().isAfter(assignment.getDeadline()) ? "OVERDUE" : "ACTIVE"
        );
        return response;
    }
}
