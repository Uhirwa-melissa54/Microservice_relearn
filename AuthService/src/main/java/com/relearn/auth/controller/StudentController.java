package com.relearn.auth.controller;

import com.relearn.auth.dto.ChangePasswordRequest;
import com.relearn.auth.dto.UserResponse;
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

/**
 * REST controller for student-specific profile operations.
 *
 * All endpoints require a valid JWT token (STUDENT role).
 * The authenticated user's identity is extracted from the JWT — students
 * can only access their own profile data.
 */
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@Tag(name = "Student Profile", description = "Student profile and account management")
@SecurityRequirement(name = "bearerAuth")
public class StudentController {

    private final UserService userService;

    /**
     * GET /api/student/me
     *
     * Returns the currently authenticated student's full profile.
     * Includes: name, email, role, className, academicYear.
     *
     * The email is extracted from the JWT — no need to pass userId in the URL.
     * This is the correct pattern for "get my own profile".
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current student profile",
               description = "Returns the authenticated student's profile including class and academic year")
    public ResponseEntity<UserResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse profile = userService.getCurrentUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    /**
     * PUT /api/student/me/password
     *
     * Changes the authenticated student's password.
     * Requires the current password for verification.
     *
     * Sample request body:
     * {
     *   "currentPassword": "oldpass123",
     *   "newPassword": "newpass456"
     * }
     */
    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change password",
               description = "Changes the authenticated user's password. Requires current password verification.")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }
}
