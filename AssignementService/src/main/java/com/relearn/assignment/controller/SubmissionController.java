package com.relearn.assignment.controller;

import com.relearn.assignment.dto.SubmissionRequest;
import com.relearn.assignment.dto.SubmissionResponse;
import com.relearn.assignment.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for submission management.
 */
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Tag(name = "Submissions", description = "Assignment submission endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SubmissionController {

    private final SubmissionService submissionService;

    /**
     * POST /api/submissions
     *
     * Submit an assignment. Automatically detects late submissions.
     * Returns 409 if already submitted (use PUT to update).
     *
     * Sample request:
     * {
     *   "assignmentId": 1,
     *   "studentId": 5,
     *   "submissionText": "My answer is...",
     *   "fileUrl": "https://storage.relearn.com/submissions/file.pdf"
     * }
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit an assignment",
               description = "Submit text or file answer. Status is auto-set to LATE if past deadline.")
    public ResponseEntity<SubmissionResponse> submitAssignment(
            @Valid @RequestBody SubmissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(submissionService.submitAssignment(request));
    }

    /**
     * PUT /api/submissions/{id}
     *
     * Update an existing submission (resubmit before deadline).
     * Only the student who made the submission can update it.
     * Not allowed after the assignment deadline.
     *
     * Query param: studentId — the ID of the student making the update.
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update a submission",
               description = "Resubmit before the deadline. Only allowed before deadline passes.")
    public ResponseEntity<SubmissionResponse> updateSubmission(
            @PathVariable Long id,
            @RequestParam Long studentId,
            @Valid @RequestBody SubmissionRequest request) {
        return ResponseEntity.ok(submissionService.updateSubmission(id, request, studentId));
    }

    /**
     * GET /api/submissions
     * Returns all submissions. Teacher/Admin use.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get all submissions", description = "TEACHER or ADMIN only.")
    public ResponseEntity<List<SubmissionResponse>> getAllSubmissions() {
        return ResponseEntity.ok(submissionService.getAllSubmissions());
    }

    /**
     * GET /api/submissions/{id}
     * Returns a single submission by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get submission by ID")
    public ResponseEntity<SubmissionResponse> getSubmissionById(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.getSubmissionById(id));
    }

    /**
     * GET /api/submissions/assignment/{assignmentId}
     * Returns all submissions for an assignment. Teacher use.
     */
    @GetMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get submissions for an assignment", description = "TEACHER or ADMIN only.")
    public ResponseEntity<List<SubmissionResponse>> getSubmissionsByAssignment(
            @PathVariable Long assignmentId) {
        return ResponseEntity.ok(submissionService.getSubmissionsByAssignment(assignmentId));
    }

    /**
     * GET /api/submissions/student/{studentId}
     * Returns all submissions by a student.
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all submissions by a student")
    public ResponseEntity<List<SubmissionResponse>> getSubmissionsByStudent(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(submissionService.getSubmissionsByStudent(studentId));
    }

    /**
     * GET /api/submissions/my/{assignmentId}?studentId={studentId}
     * Returns a student's submission for a specific assignment.
     */
    @GetMapping("/my/{assignmentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my submission for an assignment",
               description = "Returns the authenticated student's submission for a specific assignment.")
    public ResponseEntity<SubmissionResponse> getMySubmission(
            @PathVariable Long assignmentId,
            @RequestParam Long studentId) {
        return ResponseEntity.ok(
                submissionService.getMySubmissionForAssignment(assignmentId, studentId));
    }
}
