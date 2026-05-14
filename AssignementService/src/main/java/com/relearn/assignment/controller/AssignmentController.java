package com.relearn.assignment.controller;

import com.relearn.assignment.dto.AssignmentRequest;
import com.relearn.assignment.dto.AssignmentResponse;
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
 * REST controller for assignment management.
 */
@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
@Tag(name = "Assignments", description = "Assignment CRUD and filtering endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AssignmentController {

    private final AssignmentService assignmentService;

    /**
     * POST /api/assignments
     * Create a new assignment. TEACHER or ADMIN only.
     *
     * Sample request:
     * {
     *   "title": "Chapter 5 Exercises",
     *   "description": "Solve exercises on page 87-92.",
     *   "deadline": "2025-06-15T23:59:00",
     *   "className": "Y1A",
     *   "courseName": "Mathematics",
     *   "teacherId": 1,
     *   "fileUrl": "https://storage.relearn.com/assignments/ch5.pdf"
     * }
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Create an assignment", description = "TEACHER or ADMIN only.")
    public ResponseEntity<AssignmentResponse> createAssignment(
            @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.createAssignment(request));
    }

    /**
     * GET /api/assignments
     * Returns all assignments. Optional filter by status (ACTIVE/OVERDUE).
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all assignments",
               description = "Returns all assignments. Each includes a computed assignmentStatus (ACTIVE/OVERDUE).")
    public ResponseEntity<List<AssignmentResponse>> getAllAssignments() {
        return ResponseEntity.ok(assignmentService.getAllAssignments());
    }

    /**
     * GET /api/assignments/{id}
     * Returns a single assignment with full details including description and file URL.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get assignment details",
               description = "Returns full assignment details including description, deadline, and file download URL.")
    public ResponseEntity<AssignmentResponse> getAssignmentById(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.getAssignmentById(id));
    }

    /**
     * GET /api/assignments/class/{className}
     * Returns all assignments for a class.
     */
    @GetMapping("/class/{className}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get assignments by class")
    public ResponseEntity<List<AssignmentResponse>> getAssignmentsByClass(
            @PathVariable String className) {
        return ResponseEntity.ok(assignmentService.getAssignmentsByClass(className));
    }

    /**
     * GET /api/assignments/course/{courseName}
     * Returns all assignments for a course.
     */
    @GetMapping("/course/{courseName}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get assignments by course")
    public ResponseEntity<List<AssignmentResponse>> getAssignmentsByCourse(
            @PathVariable String courseName) {
        return ResponseEntity.ok(assignmentService.getAssignmentsByCourse(courseName));
    }

    /**
     * PUT /api/assignments/{id}
     * Update an assignment. TEACHER or ADMIN only.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Update an assignment", description = "TEACHER or ADMIN only.")
    public ResponseEntity<AssignmentResponse> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.ok(assignmentService.updateAssignment(id, request));
    }

    /**
     * DELETE /api/assignments/{id}
     * Delete an assignment. TEACHER or ADMIN only.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Delete an assignment", description = "TEACHER or ADMIN only.")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        assignmentService.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }
}
