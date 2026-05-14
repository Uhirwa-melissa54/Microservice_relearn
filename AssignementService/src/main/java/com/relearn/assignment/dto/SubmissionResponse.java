package com.relearn.assignment.dto;

import com.relearn.assignment.entity.Submission;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for returning submission data in API responses.
 * Never exposes the entity directly.
 */
@Getter
@Setter
public class SubmissionResponse {

    private Long id;
    private Long assignmentId;
    private String assignmentTitle;   // included for convenience — avoids a second API call
    private Long studentId;
    private String submissionText;
    private String fileUrl;
    private LocalDateTime submittedAt;
    private String status;

    /**
     * Static factory method — converts a Submission entity to a response DTO.
     *
     * @param submission the entity from the database
     * @return a clean DTO safe to return to the client
     */
    public static SubmissionResponse fromEntity(Submission submission) {
        SubmissionResponse response = new SubmissionResponse();
        response.setId(submission.getId());
        response.setAssignmentId(submission.getAssignment().getId());
        response.setAssignmentTitle(submission.getAssignment().getTitle());
        response.setStudentId(submission.getStudentId());
        response.setSubmissionText(submission.getSubmissionText());
        response.setFileUrl(submission.getFileUrl());
        response.setSubmittedAt(submission.getSubmittedAt());
        response.setStatus(submission.getStatus().name());
        return response;
    }
}
