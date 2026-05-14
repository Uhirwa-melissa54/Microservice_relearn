package com.relearn.assignment.controller;

import com.relearn.assignment.dto.AssignmentResponse;
import com.relearn.assignment.dto.AssignmentsByCourseResponse;
import com.relearn.assignment.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Student-facing assignment endpoints.
 *
 * Provides:
 *  - Assignments grouped by course (main assignments view)
 *  - Filtered by status: ACTIVE (pending) or OVERDUE
 *  - Recent assignments (dashboard widget)
 *  - History by academic year
 *
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/student/assignments")
@RequiredArgsConstructor
@Tag(name = "Student - Assignments", description = "Student assignment viewing and filtering endpoints")
@SecurityRequirement(name = "bearerAuth")
public class StudentAssignmentController {

    private final AssignmentService assignmentService;

    /**
     * GET /api/student/assignments/class/{className}
     *
     * Returns all assignments for a class, grouped by course.
     * Optional filter: academicYear
     *
     * This is the main assignments view for students.
     */
    @GetMapping("/class/{className}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get assignments grouped by course",
               description = "Returns all assignments for a class grouped by course. " +
                             "Each assignment includes computed status (ACTIVE/OVERDUE).")
    public ResponseEntity<List<AssignmentsByCourseResponse>> getAssignmentsByClassGroupedByCourse(
            @PathVariable String className,
            @Parameter(description = "Filter by academic year, e.g. 2024-2025")
            @RequestParam(required = false) String academicYear) {
        return ResponseEntity.ok(
                assignmentService.getAssignmentsGroupedByCourse(className, academicYear));
    }

    /**
     * GET /api/student/assignments/class/{className}/active
     *
     * Returns only ACTIVE (not yet overdue) assignments for a class.
     * These are the student's pending assignments.
     */
    @GetMapping("/class/{className}/active")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get active (pending) assignments",
               description = "Returns assignments whose deadline has not yet passed.")
    public ResponseEntity<List<AssignmentResponse>> getActiveAssignments(
            @PathVariable String className) {
        return ResponseEntity.ok(assignmentService.getActiveAssignmentsByClass(className));
    }

    /**
     * GET /api/student/assignments/class/{className}/overdue
     *
     * Returns only OVERDUE assignments for a class.
     * Deadline has already passed.
     */
    @GetMapping("/class/{className}/overdue")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get overdue assignments",
               description = "Returns assignments whose deadline has already passed.")
    public ResponseEntity<List<AssignmentResponse>> getOverdueAssignments(
            @PathVariable String className) {
        return ResponseEntity.ok(assignmentService.getOverdueAssignmentsByClass(className));
    }

    /**
     * GET /api/student/assignments/recent/{className}
     *
     * Returns the N most recently created assignments for a class.
     * Used for the dashboard "pending assignments" widget.
     */
    @GetMapping("/recent/{className}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get recent assignments",
               description = "Returns the N most recently created assignments for a class. Default limit is 5.")
    public ResponseEntity<List<AssignmentResponse>> getRecentAssignments(
            @PathVariable String className,
            @Parameter(description = "Number of recent assignments to return (default 5)")
            @RequestParam(defaultValue = "5") int limit) {
        int safeLimit = Math.min(limit, 20);
        return ResponseEntity.ok(
                assignmentService.getRecentAssignmentsByClass(className, safeLimit));
    }

    /**
     * GET /api/student/assignments/history/{className}/{academicYear}
     *
     * Returns all assignments for a class in a specific academic year, grouped by course.
     * Used for the academic history feature.
     *
     * Example: GET /api/student/assignments/history/Y1A/2023-2024
     */
    @GetMapping("/history/{className}/{academicYear}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get assignments by academic year (history)",
               description = "Returns all assignments for a class in a specific academic year, grouped by course.")
    public ResponseEntity<List<AssignmentsByCourseResponse>> getAssignmentsByClassAndYear(
            @PathVariable String className,
            @PathVariable String academicYear) {
        return ResponseEntity.ok(
                assignmentService.getAssignmentsGroupedByCourse(className, academicYear));
    }
}
