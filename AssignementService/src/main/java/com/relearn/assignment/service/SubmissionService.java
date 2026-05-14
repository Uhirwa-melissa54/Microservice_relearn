package com.relearn.assignment.service;

import com.relearn.assignment.dto.SubmissionRequest;
import com.relearn.assignment.dto.SubmissionResponse;
import com.relearn.assignment.entity.Assignment;
import com.relearn.assignment.entity.Submission;
import com.relearn.assignment.enums.SubmissionStatus;
import com.relearn.assignment.exception.DuplicateSubmissionException;
import com.relearn.assignment.exception.ResourceNotFoundException;
import com.relearn.assignment.repository.AssignmentRepository;
import com.relearn.assignment.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic layer for submission management.
 */
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;

    // ----------------------------------------------------------------
    //  Submit Assignment
    // ----------------------------------------------------------------

    /**
     * Records a student's submission for an assignment.
     *
     * Business rules:
     *  1. Assignment must exist
     *  2. Student cannot submit the same assignment twice (use updateSubmission instead)
     *  3. If submitted after the deadline → status = LATE, otherwise PENDING
     */
    @Transactional
    public SubmissionResponse submitAssignment(SubmissionRequest request) {
        Assignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assignment not found with id: " + request.getAssignmentId()));

        boolean alreadySubmitted = submissionRepository
                .findByAssignmentIdAndStudentId(request.getAssignmentId(), request.getStudentId())
                .isPresent();

        if (alreadySubmitted) {
            throw new DuplicateSubmissionException(
                    "Already submitted. Use PUT /api/submissions/{id} to update your submission.");
        }

        SubmissionStatus status = LocalDateTime.now().isAfter(assignment.getDeadline())
                ? SubmissionStatus.LATE
                : SubmissionStatus.PENDING;

        Submission submission = Submission.builder()
                .assignment(assignment)
                .studentId(request.getStudentId())
                .submissionText(request.getSubmissionText())
                .fileUrl(request.getFileUrl())
                .status(status)
                .build();

        return SubmissionResponse.fromEntity(submissionRepository.save(submission));
    }

    // ----------------------------------------------------------------
    //  Update Submission (resubmit before deadline)
    // ----------------------------------------------------------------

    /**
     * Updates an existing submission.
     * Only allowed if the assignment deadline has not passed.
     *
     * @param submissionId the ID of the submission to update
     * @param request      new submission content
     * @param studentId    the student making the update (for ownership check)
     */
    @Transactional
    public SubmissionResponse updateSubmission(Long submissionId, SubmissionRequest request, Long studentId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Submission not found with id: " + submissionId));

        // Ownership check — students can only update their own submissions
        if (!submission.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("You can only update your own submissions.");
        }

        // Deadline check — cannot resubmit after deadline
        if (LocalDateTime.now().isAfter(submission.getAssignment().getDeadline())) {
            throw new IllegalArgumentException(
                    "Cannot update submission — the assignment deadline has passed.");
        }

        // Update content
        submission.setSubmissionText(request.getSubmissionText());
        submission.setFileUrl(request.getFileUrl());
        // Status stays PENDING (it was on time originally)

        return SubmissionResponse.fromEntity(submissionRepository.save(submission));
    }

    // ----------------------------------------------------------------
    //  Get All Submissions
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getAllSubmissions() {
        return submissionRepository.findAll()
                .stream()
                .map(SubmissionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    //  Get Submission By ID
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public SubmissionResponse getSubmissionById(Long id) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Submission not found with id: " + id));
        return SubmissionResponse.fromEntity(submission);
    }

    // ----------------------------------------------------------------
    //  Get Submissions By Assignment
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsByAssignment(Long assignmentId) {
        if (!assignmentRepository.existsById(assignmentId)) {
            throw new ResourceNotFoundException("Assignment not found with id: " + assignmentId);
        }
        return submissionRepository.findByAssignmentId(assignmentId)
                .stream()
                .map(SubmissionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    //  Get Submissions By Student
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsByStudent(Long studentId) {
        return submissionRepository.findByStudentId(studentId)
                .stream()
                .map(SubmissionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    //  Get Student's Submission for a Specific Assignment
    // ----------------------------------------------------------------

    /**
     * Returns a student's submission for a specific assignment.
     * Used to check submission status on the assignment detail page.
     */
    @Transactional(readOnly = true)
    public SubmissionResponse getMySubmissionForAssignment(Long assignmentId, Long studentId) {
        return submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .map(SubmissionResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No submission found for assignment " + assignmentId +
                        " by student " + studentId));
    }

    // ----------------------------------------------------------------
    //  Count helpers (used by dashboard)
    // ----------------------------------------------------------------

    public long countPendingByStudent(Long studentId) {
        return submissionRepository.countByStudentIdAndStatus(studentId, SubmissionStatus.PENDING);
    }

    public long countCompletedByStudent(Long studentId) {
        return submissionRepository.countByStudentIdAndStatus(studentId, SubmissionStatus.COMPLETED);
    }

    public long countLateByStudent(Long studentId) {
        return submissionRepository.countByStudentIdAndStatus(studentId, SubmissionStatus.LATE);
    }
}
