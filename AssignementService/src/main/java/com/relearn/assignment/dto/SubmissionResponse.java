package com.relearn.assignment.dto;

import com.relearn.assignment.entity.Submission;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for returning submission data in API responses.
 * Includes grading fields so students can see their grade and feedback.
 */
@Getter
@Setter
public class SubmissionResponse {

    private Long id;
    private Long assignmentId;
    private String assignmentTitle;
    private String assignmentClassName;
    private String assignmentCourseName;
    private Long studentId;
    private String submissionText;
    private String fileUrl;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
    private String status;

    // Grading fields — null until teacher grades
    private Double score;
    private Double maxScore;
    private String feedback;
    private LocalDateTime gradedAt;
    private Long gradedBy;

    public static SubmissionResponse fromEntity(Submission submission) {
        SubmissionResponse r = new SubmissionResponse();
        r.setId(submission.getId());
        r.setAssignmentId(submission.getAssignment().getId());
        r.setAssignmentTitle(submission.getAssignment().getTitle());
        r.setAssignmentClassName(submission.getAssignment().getClassName());
        r.setAssignmentCourseName(submission.getAssignment().getCourseName());
        r.setStudentId(submission.getStudentId());
        r.setSubmissionText(submission.getSubmissionText());
        r.setFileUrl(submission.getFileUrl());
        r.setSubmittedAt(submission.getSubmittedAt());
        r.setUpdatedAt(submission.getUpdatedAt());
        r.setStatus(submission.getStatus().name());
        r.setScore(submission.getScore());
        r.setMaxScore(submission.getMaxScore());
        r.setFeedback(submission.getFeedback());
        r.setGradedAt(submission.getGradedAt());
        r.setGradedBy(submission.getGradedBy());
        return r;
    }
}
