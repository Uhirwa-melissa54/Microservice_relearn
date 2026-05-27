package com.relearn.assignment.controller;

import com.relearn.assignment.dto.AdminAssignmentStatsResponse;
import com.relearn.assignment.dto.AssignmentResponse;
import com.relearn.assignment.dto.SubmissionResponse;
import com.relearn.assignment.enums.SubmissionStatus;
import com.relearn.assignment.repository.AssignmentRepository;
import com.relearn.assignment.repository.SubmissionRepository;
import com.relearn.assignment.service.AssignmentService;
import com.relearn.assignment.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin-facing assignment and submission endpoints.
 *
 * Provides system-wide statistics and full visibility into all
 * assignments and submissions — not scoped to any single teacher.
 *
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/assignments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Assignments", description = "System-wide assignment and submission statistics")
@SecurityRequirement(name = "bearerAuth")
public class AdminAssignmentController {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final AssignmentService    assignmentService;
    private final SubmissionService    submissionService;

    // ----------------------------------------------------------------
    //  GET /api/admin/assignments/stats
    //  System-wide assignment statistics
    // ----------------------------------------------------------------

    /**
     * Returns system-wide assignment and submission statistics.
     * Used by the admin dashboard to show assignment activity.
     *
     * Response:
     * {
     *   "totalAssignments": 120,
     *   "activeAssignments": 45,
     *   "overdueAssignments": 75,
     *   "totalSubmissions": 890,
     *   "pendingReviews": 120,
     *   "gradedSubmissions": 650,
     *   "lateSubmissions": 80
     * }
     */
    @GetMapping("/stats")
    @Operation(summary = "Get system-wide assignment statistics",
               description = "Returns total assignments, active/overdue counts, and submission stats.")
    public ResponseEntity<AdminAssignmentStatsResponse> getStats() {
        LocalDateTime now = LocalDateTime.now();

        long totalAssignments  = assignmentRepository.count();
        long activeAssignments = assignmentRepository.countByDeadlineAfter(now);
        long overdueAssignments = assignmentRepository.countByDeadlineBefore(now);

        long totalSubmissions  = submissionRepository.count();
        long pendingReviews    = submissionRepository.countAllPendingReviews();
        long gradedSubmissions = submissionRepository.countByStatus(SubmissionStatus.GRADED);
        long lateSubmissions   = submissionRepository.countByStatus(SubmissionStatus.LATE);

        return ResponseEntity.ok(AdminAssignmentStatsResponse.builder()
                .totalAssignments(totalAssignments)
                .activeAssignments(activeAssignments)
                .overdueAssignments(overdueAssignments)
                .totalSubmissions(totalSubmissions)
                .pendingReviews(pendingReviews)
                .gradedSubmissions(gradedSubmissions)
                .lateSubmissions(lateSubmissions)
                .build());
    }

    // ----------------------------------------------------------------
    //  GET /api/admin/assignments
    //  All assignments system-wide
    // ----------------------------------------------------------------

    /**
     * Returns all assignments in the system.
     * Admin can see every assignment regardless of teacher.
     */
    @GetMapping
    @Operation(summary = "Get all assignments (admin view)",
               description = "Returns all assignments system-wide, newest first.")
    public ResponseEntity<List<AssignmentResponse>> getAllAssignments() {
        return ResponseEntity.ok(assignmentService.getAllAssignments());
    }

    // ----------------------------------------------------------------
    //  GET /api/admin/assignments/class/{className}
    //  All assignments for a class
    // ----------------------------------------------------------------

    /**
     * Returns all assignments for a specific class.
     * Used on the admin class details page.
     */
    @GetMapping("/class/{className}")
    @Operation(summary = "Get assignments for a class")
    public ResponseEntity<List<AssignmentResponse>> getAssignmentsByClass(
            @PathVariable String className) {
        return ResponseEntity.ok(assignmentService.getAssignmentsByClass(className));
    }

    // ----------------------------------------------------------------
    //  GET /api/admin/assignments/{id}
    //  Single assignment details
    // ----------------------------------------------------------------

    /**
     * Returns full details of a single assignment.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get assignment by ID")
    public ResponseEntity<AssignmentResponse> getAssignmentById(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.getAssignmentById(id));
    }

    // ----------------------------------------------------------------
    //  GET /api/admin/assignments/{id}/submissions
    //  All submissions for an assignment
    // ----------------------------------------------------------------

    /**
     * Returns all submissions for a specific assignment.
     * Admin can see all submissions regardless of teacher.
     */
    @GetMapping("/{id}/submissions")
    @Operation(summary = "Get all submissions for an assignment")
    public ResponseEntity<List<SubmissionResponse>> getSubmissionsForAssignment(
            @PathVariable Long id) {
        return ResponseEntity.ok(submissionService.getSubmissionsForAssignment(id, null));
    }

    // ----------------------------------------------------------------
    //  DELETE /api/admin/assignments/{id}
    //  Admin force-delete an assignment
    // ----------------------------------------------------------------

    /**
     * Admin permanently deletes an assignment and all its submissions.
     * Use with caution.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an assignment (admin)",
               description = "Permanently deletes an assignment and all its submissions.")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        assignmentService.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }
}
