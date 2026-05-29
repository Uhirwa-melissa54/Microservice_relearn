package com.relearn.assignment.controller;

import com.relearn.assignment.dto.*;
import com.relearn.assignment.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * Teacher-facing assignment endpoints.
 *
 * All endpoints require TEACHER or ADMIN role.
 * Teachers can only manage their own assignments (enforced by teacherId param).
 */
@RestController
@RequestMapping("/api/teacher/assignments")
@RequiredArgsConstructor
@Tag(name = "Teacher - Assignments", description = "Teacher assignment management and dashboard")
@SecurityRequirement(name = "bearerAuth")
public class TeacherAssignmentController {

    private final AssignmentService assignmentService;

    // ----------------------------------------------------------------
    //  GET /api/teacher/assignments/dashboard/{teacherId}
    //  Teacher dashboard — aggregated stats + class cards + recent assignments
    // ----------------------------------------------------------------

    /**
     * Returns the complete teacher dashboard.
     *
     * Response includes:
     * - totalClassAssignments (distinct class+course combos)
     * - totalAssignmentsGiven
     * - totalPendingReviews
     * - classCards (one per class+course)
     * - recentAssignments (last 5 with submission stats)
     */
    @GetMapping("/dashboard/{teacherId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get teacher dashboard",
               description = "Returns aggregated dashboard data: stats, class cards (with student counts), and recent assignments.")
    public ResponseEntity<TeacherDashboardResponse> getTeacherDashboard(
            @PathVariable Long teacherId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Extract the raw token (strip "Bearer " prefix) to forward to Auth Service
        String jwtToken = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : null;
        return ResponseEntity.ok(assignmentService.getTeacherDashboard(teacherId, jwtToken));
    }

    // ----------------------------------------------------------------
    //  GET /api/teacher/assignments/{teacherId}
    //  All assignments by teacher, ordered: ACTIVE → OVERDUE
    // ----------------------------------------------------------------

    /**
     * Returns all assignments created by this teacher.
     * Ordered: active assignments first, then overdue.
     * Each includes submission stats (submitted count, pending review, graded).
     */
    @GetMapping("/{teacherId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get teacher's assignments",
               description = "Returns all assignments ordered: ACTIVE first, then OVERDUE. " +
                             "Each includes submission statistics.")
    public ResponseEntity<List<AssignmentWithStatsResponse>> getTeacherAssignments(
            @PathVariable Long teacherId) {
        return ResponseEntity.ok(assignmentService.getTeacherAssignments(teacherId));
    }

    // ----------------------------------------------------------------
    //  GET /api/teacher/assignments/{teacherId}/class/{className}/course/{courseName}
    //  Assignments for a specific class+course
    // ----------------------------------------------------------------

    /**
     * Returns all assignments for a specific class+course taught by this teacher.
     * Used on the class details page.
     */
    @GetMapping("/{teacherId}/class/{className}/course/{courseName}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get assignments by class and course",
               description = "Returns assignments for a specific class+course combination.")
    public ResponseEntity<List<AssignmentWithStatsResponse>> getAssignmentsByClassAndCourse(
            @PathVariable Long teacherId,
            @PathVariable String className,
            @PathVariable String courseName) {
        return ResponseEntity.ok(
                assignmentService.getTeacherAssignmentsByClassAndCourse(
                        teacherId, className, courseName));
    }

    // ----------------------------------------------------------------
    //  GET /api/teacher/assignments/{assignmentId}/stats?totalExpected=35
    //  Submission statistics for a single assignment
    // ----------------------------------------------------------------

    /**
     * Returns submission statistics for an assignment.
     * Used as the header stats on the submissions page.
     *
     * Pass totalExpected = total students in the class.
     */
    @GetMapping("/{assignmentId}/stats")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get assignment submission stats",
               description = "Returns total expected, submitted, graded, pending, late, and missing counts.")
    public ResponseEntity<SubmissionStatsResponse> getAssignmentStats(
            @PathVariable Long assignmentId,
            @Parameter(description = "Total students expected to submit (class size)")
            @RequestParam(defaultValue = "0") long totalExpected) {
        return ResponseEntity.ok(
                assignmentService.getAssignmentSubmissionStats(assignmentId, totalExpected));
    }

    // ----------------------------------------------------------------
    //  POST /api/teacher/assignments
    //  Create assignment
    // ----------------------------------------------------------------

    /**
     * Creates a new assignment.
     *
     * Sample request:
     * {
     *   "title": "Chapter 5 Exercises",
     *   "description": "Solve exercises on page 87-92. Show all working.",
     *   "deadline": "2025-06-15T23:59:00",
     *   "className": "Y1A",
     *   "courseName": "Mathematics",
     *   "academicYear": "2024-2025",
     *   "teacherId": 1,
     *   "fileUrl": "https://storage.relearn.com/assignments/ch5.pdf",
     *   "submissionType": "BOTH"
     * }
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Create an assignment",
               description = "submissionType: FILE_ONLY | TEXT_ONLY | BOTH (default: BOTH)")
    public ResponseEntity<AssignmentResponse> createAssignment(
            @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.createAssignment(request));
    }

    // ----------------------------------------------------------------
    //  PUT /api/teacher/assignments/{id}
    //  Update assignment
    // ----------------------------------------------------------------

    /**
     * Updates an existing assignment.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Update an assignment")
    public ResponseEntity<AssignmentResponse> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.ok(assignmentService.updateAssignment(id, request));
    }

    // ----------------------------------------------------------------
    //  DELETE /api/teacher/assignments/{id}
    //  Delete assignment
    // ----------------------------------------------------------------

    /**
     * Permanently deletes an assignment and all its submissions.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Delete an assignment")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        assignmentService.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }
}
