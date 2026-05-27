package com.relearn.assignment.service;

import com.relearn.assignment.dto.GradeSubmissionRequest;
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
 * Handles student submissions and teacher grading.
 */
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;

    // ================================================================
    //  STUDENT FEATURES
    // ================================================================

    /**
     * Records a student's submission.
     * Auto-detects late submissions.
     */
    @Transactional
    public SubmissionResponse submitAssignment(SubmissionRequest request) {
        Assignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Assignment not found with id: " + request.getAssignmentId()));

        if (submissionRepository.findByAssignmentIdAndStudentId(
                request.getAssignmentId(), request.getStudentId()).isPresent()) {
            throw new DuplicateSubmissionException(
                    "Already submitted. Use PUT /api/submissions/{id} to update.");
        }

        SubmissionStatus status = LocalDateTime.now().isAfter(assignment.getDeadline())
                ? SubmissionStatus.LATE : SubmissionStatus.PENDING;

        Submission submission = Submission.builder()
                .assignment(assignment)
                .studentId(request.getStudentId())
                .submissionText(request.getSubmissionText())
                .fileUrl(request.getFileUrl())
                .status(status)
                .build();

        return SubmissionResponse.fromEntity(submissionRepository.save(submission));
    }

    /**
     * Updates an existing submission (resubmit before deadline).
     * Only the owning student can update. Not allowed after deadline.
     */
    @Transactional
    public SubmissionResponse updateSubmission(Long submissionId, SubmissionRequest request,
                                               Long studentId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Submission not found with id: " + submissionId));

        if (!submission.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("You can only update your own submissions.");
        }
        if (LocalDateTime.now().isAfter(submission.getAssignment().getDeadline())) {
            throw new IllegalArgumentException(
                    "Cannot update submission — the assignment deadline has passed.");
        }

        submission.setSubmissionText(request.getSubmissionText());
        submission.setFileUrl(request.getFileUrl());

        return SubmissionResponse.fromEntity(submissionRepository.save(submission));
    }

    // ================================================================
    //  TEACHER FEATURES
    // ================================================================

    /**
     * Grades a submission.
     * Sets score, feedback, gradedAt, gradedBy, and updates status to GRADED.
     *
     * @param submissionId the submission to grade
     * @param request      grade data from the teacher
     * @param teacherId    the teacher performing the grading (for ownership check)
     */
    @Transactional
    public SubmissionResponse gradeSubmission(Long submissionId, GradeSubmissionRequest request,
                                              Long teacherId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Submission not found with id: " + submissionId));

        // Verify the teacher owns this assignment
        if (!submission.getAssignment().getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException(
                    "You can only grade submissions for your own assignments.");
        }

        // Validate score doesn't exceed max
        if (request.getScore() > request.getMaxScore()) {
            throw new IllegalArgumentException(
                    "Score (" + request.getScore() + ") cannot exceed max score (" +
                    request.getMaxScore() + ").");
        }

        submission.setScore(request.getScore());
        submission.setMaxScore(request.getMaxScore());
        submission.setFeedback(request.getFeedback());
        submission.setGradedAt(LocalDateTime.now());
        submission.setGradedBy(request.getGradedBy());
        submission.setStatus(SubmissionStatus.GRADED);

        return SubmissionResponse.fromEntity(submissionRepository.save(submission));
    }

    /**
     * Returns all submissions for an assignment, optionally filtered by status.
     * Used by the teacher's submissions page.
     *
     * @param assignmentId the assignment to view submissions for
     * @param status       optional filter: PENDING, LATE, GRADED (null = all)
     */
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsForAssignment(Long assignmentId, String status) {
        if (!assignmentRepository.existsById(assignmentId)) {
            throw new ResourceNotFoundException("Assignment not found with id: " + assignmentId);
        }

        if (status != null && !status.isBlank()) {
            SubmissionStatus statusEnum = SubmissionStatus.valueOf(status.toUpperCase());
            return submissionRepository.findByAssignmentIdAndStatus(assignmentId, statusEnum)
                    .stream().map(SubmissionResponse::fromEntity).collect(Collectors.toList());
        }

        return submissionRepository.findByAssignmentId(assignmentId)
                .stream().map(SubmissionResponse::fromEntity).collect(Collectors.toList());
    }

    /**
     * Returns all pending review submissions for a teacher.
     * Used for the teacher dashboard pending reviews count.
     */
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getPendingReviewsForTeacher(Long teacherId) {
        return submissionRepository.findPendingReviewByTeacherId(teacherId)
                .stream().map(SubmissionResponse::fromEntity).collect(Collectors.toList());
    }

    // ================================================================
    //  SHARED
    // ================================================================

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getAllSubmissions() {
        return submissionRepository.findAll()
                .stream().map(SubmissionResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getSubmissionById(Long id) {
        return SubmissionResponse.fromEntity(
                submissionRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Submission not found with id: " + id)));
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsByAssignment(Long assignmentId) {
        if (!assignmentRepository.existsById(assignmentId)) {
            throw new ResourceNotFoundException("Assignment not found with id: " + assignmentId);
        }
        return submissionRepository.findByAssignmentId(assignmentId)
                .stream().map(SubmissionResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsByStudent(Long studentId) {
        return submissionRepository.findByStudentId(studentId)
                .stream().map(SubmissionResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getMySubmissionForAssignment(Long assignmentId, Long studentId) {
        return submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .map(SubmissionResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No submission found for assignment " + assignmentId +
                        " by student " + studentId));
    }

    // Count helpers
    public long countPendingByStudent(Long studentId) {
        return submissionRepository.countByStudentIdAndStatus(studentId, SubmissionStatus.PENDING);
    }
    public long countGradedByStudent(Long studentId) {
        return submissionRepository.countByStudentIdAndStatus(studentId, SubmissionStatus.GRADED);
    }
    public long countLateByStudent(Long studentId) {
        return submissionRepository.countByStudentIdAndStatus(studentId, SubmissionStatus.LATE);
    }
}
