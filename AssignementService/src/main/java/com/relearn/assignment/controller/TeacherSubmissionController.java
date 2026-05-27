package com.relearn.assignment.controller;

import com.relearn.assignment.dto.GradeSubmissionRequest;
import com.relearn.assignment.dto.SubmissionResponse;
import com.relearn.assignment.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Teacher-facing submission endpoints.
 *
 * Teachers can:
 * - View all submissions for their assignments (with status filter)
 * - View a single submission (text + file)
 * - Grade a submission (score + feedback)
 * - View their pending reviews
 */
@RestController
@RequestMapping("/api/teacher/submissions")
@RequiredArgsConstructor
@Tag(name = "Teacher - Submissions", description = "Teacher submission review and grading")
@SecurityRequirement(name = "bearerAuth")
public class TeacherSubmissionController {

    private final SubmissionService submissionService;

    // ----------------------------------------------------------------
    //  GET /api/teacher/submissions/assignment/{assignmentId}
    //  All submissions for an assignment (with optional status filter)
    // ----------------------------------------------------------------

    /**
     * Returns all submissions for a specific assignment.
     * Optional filter by status: PENDING, LATE, GRADED
     *
     * This is the main submissions table on the teacher's assignment page.
     *
     * Example:
     * GET /api/teacher/submissions/assignment/1
     * GET /api/teacher/submissions/assignment/1?status=PENDING
     * GET /api/teacher/submissions/assignment/1?status=GRADED
     */
    @GetMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get submissions for an assignment",
               description = "Returns all submissions. Filter by status: PENDING | LATE | GRADED")
    public ResponseEntity<List<SubmissionResponse>> getSubmissionsForAssignment(
            @PathVariable Long assignmentId,
            @Parameter(description = "Filter by status: PENDING, LATE, GRADED (omit for all)")
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(
                submissionService.getSubmissionsForAssignment(assignmentId, status));
    }

    // ----------------------------------------------------------------
    //  GET /api/teacher/submissions/{id}
    //  View a single submission (text + file URL)
    // ----------------------------------------------------------------

    /**
     * Returns full details of a single submission.
     * Teacher uses this to read the student's text answer and/or download their file.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "View a submission",
               description = "Returns full submission details including text answer, file URL, and grading info.")
    public ResponseEntity<SubmissionResponse> getSubmissionById(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.getSubmissionById(id));
    }

    // ----------------------------------------------------------------
    //  GET /api/teacher/submissions/pending/{teacherId}
    //  All pending reviews for a teacher
    // ----------------------------------------------------------------

    /**
     * Returns all submissions waiting for grading by this teacher.
     * Status: PENDING or LATE (not yet graded).
     */
    @GetMapping("/pending/{teacherId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get pending reviews",
               description = "Returns all PENDING and LATE submissions across all teacher's assignments.")
    public ResponseEntity<List<SubmissionResponse>> getPendingReviews(
            @PathVariable Long teacherId) {
        return ResponseEntity.ok(submissionService.getPendingReviewsForTeacher(teacherId));
    }

    // ----------------------------------------------------------------
    //  PUT /api/teacher/submissions/{id}/grade
    //  Grade a submission
    // ----------------------------------------------------------------

    /**
     * Grades a student's submission.
     * Sets score, maxScore, feedback, and updates status to GRADED.
     *
     * Sample request:
     * {
     *   "score": 85.5,
     *   "maxScore": 100.0,
     *   "feedback": "Good work! Your explanation of quadratic formula was clear.",
     *   "gradedBy": 1
     * }
     *
     * After grading, the student can see their grade and feedback.
     */
    @PutMapping("/{id}/grade")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Grade a submission",
               description = "Assigns a score and optional feedback. Status is set to GRADED.")
    public ResponseEntity<SubmissionResponse> gradeSubmission(
            @PathVariable Long id,
            @RequestParam Long teacherId,
            @Valid @RequestBody GradeSubmissionRequest request) {
        return ResponseEntity.ok(
                submissionService.gradeSubmission(id, request, teacherId));
    }
}
