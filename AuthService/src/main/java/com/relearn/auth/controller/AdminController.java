package com.relearn.auth.controller;

import com.relearn.auth.dto.*;
import com.relearn.auth.enums.Role;
import com.relearn.auth.service.ActivityLogService;
import com.relearn.auth.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin portal REST controller.
 *
 * All endpoints require ADMIN role.
 * The authenticated admin's identity is extracted from the JWT.
 *
 * Base path: /api/admin
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Portal", description = "Admin dashboard, user management, class management, and activity logs")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;
    private final ActivityLogService activityLogService;

    // ================================================================
    //  DASHBOARD
    // ================================================================

    /**
     * GET /api/admin/dashboard
     *
     * Returns the complete admin dashboard in a single call.
     *
     * Response includes:
     * - totalStudents, totalTeachers, totalActiveClasses
     * - systemActivityPercentage (0-100)
     * - newUsersThisWeek
     * - activeUsers
     * - recentActivities (last 20 events)
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard",
               description = "Returns aggregated system stats and recent activity feed.")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    // ================================================================
    //  USER MANAGEMENT
    // ================================================================

    /**
     * GET /api/admin/users
     *
     * Returns paginated, searchable list of all users.
     *
     * Query params:
     * - role: ADMIN | TEACHER | STUDENT (optional)
     * - search: name or email substring (optional)
     * - activeOnly: true | false (default false = all users)
     * - page: page number (default 0)
     * - size: page size (default 20)
     *
     * Examples:
     * GET /api/admin/users
     * GET /api/admin/users?role=STUDENT&search=john&page=0&size=20
     * GET /api/admin/users?role=TEACHER&activeOnly=true
     */
    @GetMapping("/users")
    @Operation(summary = "Get all users (paginated)",
               description = "Supports filtering by role, search by name/email, and active status.")
    public ResponseEntity<PagedResponse<UserResponse>> getUsers(
            @Parameter(description = "Filter by role: ADMIN, TEACHER, STUDENT")
            @RequestParam(required = false) String role,
            @Parameter(description = "Search by name or email")
            @RequestParam(required = false) String search,
            @Parameter(description = "Only return active users")
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                adminService.getUsers(role, search, activeOnly, page, size));
    }

    /**
     * GET /api/admin/users/students
     *
     * Shortcut — returns paginated list of students only.
     */
    @GetMapping("/users/students")
    @Operation(summary = "Get all students (paginated)")
    public ResponseEntity<PagedResponse<UserResponse>> getStudents(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                adminService.getUsers("STUDENT", search, false, page, size));
    }

    /**
     * GET /api/admin/users/teachers
     *
     * Shortcut — returns paginated list of teachers only.
     */
    @GetMapping("/users/teachers")
    @Operation(summary = "Get all teachers (paginated)")
    public ResponseEntity<PagedResponse<UserResponse>> getTeachers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                adminService.getUsers("TEACHER", search, false, page, size));
    }

    /**
     * GET /api/admin/users/{id}
     *
     * Returns a single user by ID.
     */
    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    /**
     * POST /api/admin/users
     *
     * Admin creates a new user account.
     *
     * Sample request:
     * {
     *   "fullName": "Alice Johnson",
     *   "email": "alice@relearn.com",
     *   "password": "secure123",
     *   "role": "STUDENT",
     *   "className": "Y1A",
     *   "academicYear": "2024-2025"
     * }
     */
    @PostMapping("/users")
    @Operation(summary = "Create a user",
               description = "Admin creates a student, teacher, or admin account.")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody AdminCreateUserRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse admin = adminService.getUserById(extractAdminId(userDetails));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createUser(request, admin.getId(), admin.getFullName()));
    }

    /**
     * PUT /api/admin/users/{id}
     *
     * Admin updates a user's info (full replacement).
     *
     * Sample request:
     * {
     *   "fullName": "Alice Johnson",
     *   "email": "alice@relearn.com",
     *   "role": "STUDENT",
     *   "className": "Y2A",
     *   "academicYear": "2025-2026",
     *   "active": true
     * }
     */
    @PutMapping("/users/{id}")
    @Operation(summary = "Update a user",
               description = "Updates all user fields. Use PATCH /users/{id}/role for role-only changes.")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateUserRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse admin = adminService.getUserById(extractAdminId(userDetails));
        return ResponseEntity.ok(
                adminService.updateUser(id, request, admin.getId(), admin.getFullName()));
    }

    /**
     * PATCH /api/admin/users/{id}/role
     *
     * Assigns a new role to a user.
     *
     * Sample request: { "role": "TEACHER" }
     */
    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Assign role to user")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Role newRole = Role.valueOf(body.get("role").toUpperCase());
        UserResponse admin = adminService.getUserById(extractAdminId(userDetails));
        return ResponseEntity.ok(
                adminService.assignRole(id, newRole, admin.getId(), admin.getFullName()));
    }

    /**
     * PATCH /api/admin/users/{id}/password
     *
     * Admin resets a user's password.
     *
     * Sample request: { "newPassword": "newpass123" }
     */
    @PatchMapping("/users/{id}/password")
    @Operation(summary = "Reset user password",
               description = "Admin resets a user's password without needing the current password.")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        UserResponse admin = adminService.getUserById(extractAdminId(userDetails));
        adminService.resetUserPassword(id, newPassword, admin.getId(), admin.getFullName());
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/admin/users/{id}/deactivate
     *
     * Soft-deletes a user (sets active = false).
     * The user can no longer log in but their data is preserved.
     */
    @PatchMapping("/users/{id}/deactivate")
    @Operation(summary = "Deactivate a user (soft delete)",
               description = "Sets active=false. User cannot log in but data is preserved.")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse admin = adminService.getUserById(extractAdminId(userDetails));
        adminService.deactivateUser(id, admin.getId(), admin.getFullName());
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/admin/users/{id}/reactivate
     *
     * Reactivates a previously deactivated user.
     */
    @PatchMapping("/users/{id}/reactivate")
    @Operation(summary = "Reactivate a user",
               description = "Restores access for a previously deactivated user.")
    public ResponseEntity<Void> reactivateUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse admin = adminService.getUserById(extractAdminId(userDetails));
        adminService.reactivateUser(id, admin.getId(), admin.getFullName());
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/admin/users/{id}
     *
     * Permanently deletes a user.
     * WARNING: This is irreversible. Prefer PATCH /deactivate for most cases.
     */
    @DeleteMapping("/users/{id}")
    @Operation(summary = "Permanently delete a user",
               description = "Hard delete — irreversible. Prefer /deactivate for most cases.")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse admin = adminService.getUserById(extractAdminId(userDetails));
        adminService.hardDeleteUser(id, admin.getId(), admin.getFullName());
        return ResponseEntity.noContent().build();
    }

    // ================================================================
    //  CLASS MANAGEMENT
    // ================================================================

    /**
     * GET /api/admin/classes
     * Returns all active classes from the AcademicClass table.
     */
    @GetMapping("/classes")
    @Operation(summary = "Get all classes")
    public ResponseEntity<List<ClassOverviewResponse>> getAllClasses() {
        return ResponseEntity.ok(adminService.getAllClasses());
    }

    /**
     * GET /api/admin/classes/{className}
     * Returns details for a specific class.
     */
    @GetMapping("/classes/{className}")
    @Operation(summary = "Get class details")
    public ResponseEntity<ClassOverviewResponse> getClassDetails(
            @PathVariable String className) {
        return ResponseEntity.ok(adminService.getClassDetails(className));
    }

    /**
     * POST /api/admin/classes
     * Creates a new class.
     *
     * Sample: { "className": "Y1A", "academicYear": "2024-2025", "capacity": 40, "teacherId": 1 }
     */
    @PostMapping("/classes")
    @Operation(summary = "Create a class")
    public ResponseEntity<ClassOverviewResponse> createClass(
            @Valid @RequestBody ClassUpsertRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse admin = adminService.getUserById(extractAdminId(userDetails));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createClass(request, admin.getId(), admin.getFullName()));
    }

    /**
     * PUT /api/admin/classes/{className}
     * Updates an existing class.
     */
    @PutMapping("/classes/{className}")
    @Operation(summary = "Update a class")
    public ResponseEntity<ClassOverviewResponse> updateClass(
            @PathVariable String className,
            @Valid @RequestBody ClassUpsertRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse admin = adminService.getUserById(extractAdminId(userDetails));
        return ResponseEntity.ok(
                adminService.updateClass(className, request, admin.getId(), admin.getFullName()));
    }

    /**
     * PATCH /api/admin/classes/{className}/assign-teacher
     * Assigns a teacher to a class. Teacher sees it immediately in their dashboard.
     *
     * Sample: { "teacherId": 5 }
     */
    @PatchMapping("/classes/{className}/assign-teacher")
    @Operation(summary = "Assign teacher to class",
               description = "Teacher immediately sees this class in their dashboard.")
    public ResponseEntity<ClassOverviewResponse> assignTeacher(
            @PathVariable String className,
            @Valid @RequestBody AssignTeacherRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse admin = adminService.getUserById(extractAdminId(userDetails));
        return ResponseEntity.ok(
                adminService.assignTeacher(className, request.getTeacherId(),
                        admin.getId(), admin.getFullName()));
    }

    /**
     * DELETE /api/admin/classes/{className}
     * Soft-deletes a class (sets active = false).
     */
    @DeleteMapping("/classes/{className}")
    @Operation(summary = "Delete (deactivate) a class")
    public ResponseEntity<Void> deleteClass(
            @PathVariable String className,
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse admin = adminService.getUserById(extractAdminId(userDetails));
        adminService.deleteClass(className, admin.getId(), admin.getFullName());
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/admin/classes/{className}/students
     * Returns all students enrolled in a class.
     */
    @GetMapping("/classes/{className}/students")
    @Operation(summary = "Get students in a class")
    public ResponseEntity<List<UserResponse>> getStudentsByClass(
            @PathVariable String className) {
        return ResponseEntity.ok(adminService.getStudentsByClass(className));
    }

    /**
     * GET /api/admin/classes/teacher/{teacherId}
     * Returns all classes assigned to a specific teacher.
     * Used by the teacher dashboard to show assigned classes.
     */
    @GetMapping("/classes/teacher/{teacherId}")
    @Operation(summary = "Get classes assigned to a teacher")
    public ResponseEntity<List<ClassOverviewResponse>> getClassesByTeacher(
            @PathVariable Long teacherId) {
        return ResponseEntity.ok(adminService.getClassesByTeacher(teacherId));
    }

    /**
     * GET /api/admin/academic-years
     */
    @GetMapping("/academic-years")
    @Operation(summary = "Get all academic years")
    public ResponseEntity<List<String>> getAcademicYears() {
        return ResponseEntity.ok(adminService.getAcademicYears());
    }

    // ================================================================
    //  ACTIVITY LOGS
    // ================================================================

    /**
     * GET /api/admin/activities
     *
     * Returns paginated activity log.
     * Optional filter by type.
     *
     * Examples:
     * GET /api/admin/activities
     * GET /api/admin/activities?type=USER_REGISTERED&page=0&size=20
     * GET /api/admin/activities?type=NOTE_UPLOADED
     */
    @GetMapping("/activities")
    @Operation(summary = "Get activity log (paginated)",
               description = "Returns system activity log. Filter by type: USER_REGISTERED, " +
                             "NOTE_UPLOADED, ASSIGNMENT_CREATED, etc.")
    public ResponseEntity<PagedResponse<ActivityLogResponse>> getActivities(
            @Parameter(description = "Filter by activity type (optional)")
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(activityLogService.getActivities(type, page, size));
    }

    /**
     * GET /api/admin/activities/user/{userId}
     *
     * Returns all activities performed by a specific user.
     */
    @GetMapping("/activities/user/{userId}")
    @Operation(summary = "Get activities by user")
    public ResponseEntity<List<ActivityLogResponse>> getActivitiesByUser(
            @PathVariable Long userId) {
        return ResponseEntity.ok(activityLogService.getActivitiesByActor(userId));
    }

    /**
     * GET /api/admin/activities/entity/{entityType}/{entityId}
     *
     * Returns all activities related to a specific entity.
     * Example: GET /api/admin/activities/entity/NOTE/5
     */
    @GetMapping("/activities/entity/{entityType}/{entityId}")
    @Operation(summary = "Get activities for a specific entity",
               description = "entityType: USER, NOTE, ASSIGNMENT, CLASS")
    public ResponseEntity<List<ActivityLogResponse>> getActivitiesByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        return ResponseEntity.ok(
                activityLogService.getActivitiesByEntity(entityType, entityId));
    }

    // ================================================================
    //  Internal helper
    // ================================================================

    /**
     * Extracts the admin's user ID from the JWT principal.
     * The email is the JWT subject; we look up the user by email.
     */
    private Long extractAdminId(UserDetails userDetails) {
        return adminService.getUserByEmail(userDetails.getUsername()).getId();
    }
}
