package com.relearn.auth.controller;

import com.relearn.auth.dto.*;
import com.relearn.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for teacher-specific profile and class operations.
 *
 * Provides:
 * - Teacher profile (view, password change)
 * - Student count per class (for dashboard)
 * - Student list per class (for submissions page)
 */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@Tag(name = "Teacher - Profile", description = "Teacher profile and class management")
@SecurityRequirement(name = "bearerAuth")
public class TeacherController {

    private final UserService userService;

    // ----------------------------------------------------------------
    //  GET /api/teacher/me
    //  Current teacher's profile (from JWT)
    // ----------------------------------------------------------------

    /**
     * Returns the currently authenticated teacher's profile.
     * Includes: name, email, role, joined date.
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get teacher profile",
               description = "Returns the authenticated teacher's profile.")
    public ResponseEntity<TeacherProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                userService.getTeacherProfileByEmail(userDetails.getUsername()));
    }

    // ----------------------------------------------------------------
    //  GET /api/teacher/profile/{teacherId}
    //  Teacher profile by ID
    // ----------------------------------------------------------------

    /**
     * Returns a teacher's profile by their ID.
     * Used when displaying teacher info on notes/assignments.
     */
    @GetMapping("/profile/{teacherId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get teacher profile by ID",
               description = "Returns teacher info. Used to display teacher name on notes/assignments.")
    public ResponseEntity<TeacherProfileResponse> getTeacherProfile(
            @PathVariable Long teacherId) {
        return ResponseEntity.ok(userService.getTeacherProfile(teacherId));
    }

    // ----------------------------------------------------------------
    //  PUT /api/teacher/me/password
    //  Change password
    // ----------------------------------------------------------------

    /**
     * Changes the authenticated teacher's password.
     * Requires current password verification.
     *
     * Sample request:
     * {
     *   "currentPassword": "oldpass123",
     *   "newPassword": "newpass456"
     * }
     */
    @PutMapping("/me/password")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Change password",
               description = "Changes password. Requires current password verification.")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    // ----------------------------------------------------------------
    //  GET /api/teacher/classes/{className}/students/count
    //  Student count for a class (for dashboard class cards)
    // ----------------------------------------------------------------

    /**
     * Returns the number of students in a specific class.
     * Used by the teacher dashboard to populate class card stats.
     *
     * Example: GET /api/teacher/classes/Y1A/students/count
     * Response: { "className": "Y1A", "studentCount": 35 }
     */
    @GetMapping("/classes/{className}/students/count")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get student count for a class",
               description = "Returns total students enrolled in a class. Used for dashboard class cards.")
    public ResponseEntity<StudentCountResponse> getStudentCount(
            @PathVariable String className) {
        return ResponseEntity.ok(userService.getStudentCountByClass(className));
    }

    // ----------------------------------------------------------------
    //  GET /api/teacher/classes/{className}/students
    //  All students in a class
    // ----------------------------------------------------------------

    /**
     * Returns all students in a specific class.
     * Used on the submissions page to show which students haven't submitted.
     */
    @GetMapping("/classes/{className}/students")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(summary = "Get students in a class",
               description = "Returns all students enrolled in a class.")
    public ResponseEntity<List<UserResponse>> getStudentsByClass(
            @PathVariable String className) {
        return ResponseEntity.ok(userService.getStudentsByClass(className));
    }
}
