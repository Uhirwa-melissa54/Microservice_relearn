package com.relearn.auth.service;

import com.relearn.auth.dto.*;
import com.relearn.auth.entity.User;
import com.relearn.auth.enums.ActivityType;
import com.relearn.auth.enums.Role;
import com.relearn.auth.exception.ResourceNotFoundException;
import com.relearn.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin service — handles all admin-specific operations.
 *
 * Responsibilities:
 * - Dashboard aggregation
 * - User management (create, update, soft-delete, restore)
 * - Class overview
 * - Activity percentage calculation
 *
 * All write operations log an activity event for the audit trail.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final PasswordEncoder passwordEncoder;

    // ================================================================
    //  DASHBOARD
    // ================================================================

    /**
     * Builds the complete admin dashboard response.
     *
     * Activity percentage formula:
     *   events in last 7 days / (total active users * 2) * 100
     *   Rationale: if every active user generates ~2 events/week, that's 100% activity.
     *   Capped at 100.
     */
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        long totalStudents      = userRepository.countByRole(Role.STUDENT);
        long totalTeachers      = userRepository.countByRole(Role.TEACHER);
        long activeUsers        = userRepository.countByActive(true);
        long newUsersThisWeek   = userRepository.countByCreatedAtAfter(
                LocalDateTime.now().minusDays(7));

        // Distinct class names from students
        long totalActiveClasses = userRepository.findDistinctClassNames().size();

        // Activity percentage
        long recentEvents = activityLogService.countRecentEvents(7);
        long baseline     = Math.max(activeUsers * 2, 1); // avoid division by zero
        double activityPct = Math.min(100.0, (recentEvents * 100.0) / baseline);
        // Round to 1 decimal
        activityPct = Math.round(activityPct * 10.0) / 10.0;

        List<ActivityLogResponse> recentActivities =
                activityLogService.getRecentActivities();

        return AdminDashboardResponse.builder()
                .totalStudents(totalStudents)
                .totalTeachers(totalTeachers)
                .totalActiveClasses(totalActiveClasses)
                .systemActivityPercentage(activityPct)
                .newUsersThisWeek(newUsersThisWeek)
                .activeUsers(activeUsers)
                .recentActivities(recentActivities)
                .build();
    }

    // ================================================================
    //  USER MANAGEMENT
    // ================================================================

    /**
     * Returns paginated, searchable list of all users.
     *
     * @param role   optional role filter (null = all roles)
     * @param search optional name/email search string
     * @param page   page number (0-indexed)
     * @param size   page size (default 20)
     */
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getUsers(String role, String search,
                                                 boolean activeOnly, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<User> result;

        if (role != null && !role.isBlank()) {
            Role roleEnum = Role.valueOf(role.toUpperCase());
            if (activeOnly) {
                result = userRepository.searchUsersByRoleAndStatus(
                        roleEnum, true, search, pageable);
            } else {
                result = userRepository.searchUsersByRole(roleEnum, search, pageable);
            }
        } else {
            result = userRepository.searchUsers(search, pageable);
        }

        Page<UserResponse> mapped = result.map(UserResponse::fromEntity);
        return PagedResponse.from(mapped);
    }

    /**
     * Returns a single user by ID.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return UserResponse.fromEntity(
                userRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "User not found with id: " + id)));
    }

    /**
     * Admin creates a new user.
     * Logs USER_REGISTERED activity.
     *
     * @param request  user data
     * @param adminId  ID of the admin performing the action
     * @param adminName name of the admin
     */
    @Transactional
    public UserResponse createUser(AdminCreateUserRequest request,
                                    Long adminId, String adminName) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .className(request.getClassName())
                .academicYear(request.getAcademicYear())
                .active(true)
                .build();

        User saved = userRepository.save(user);

        // Log the activity
        activityLogService.log(
                ActivityType.USER_REGISTERED,
                String.format("Admin created %s account for %s (%s)",
                        saved.getRole().name().toLowerCase(),
                        saved.getFullName(), saved.getEmail()),
                adminId, adminName, "ADMIN",
                saved.getId(), "USER",
                "role=" + saved.getRole().name()
        );

        return UserResponse.fromEntity(saved);
    }

    /**
     * Admin updates an existing user's info.
     * Logs USER_UPDATED activity.
     */
    @Transactional
    public UserResponse updateUser(Long userId, AdminUpdateUserRequest request,
                                    Long adminId, String adminName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));

        // Check email uniqueness if it changed
        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Email already in use: " + request.getEmail());
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setClassName(request.getClassName());
        user.setAcademicYear(request.getAcademicYear());
        user.setActive(request.getActive());

        User saved = userRepository.save(user);

        activityLogService.log(
                ActivityType.USER_UPDATED,
                String.format("Admin updated user %s (%s)", saved.getFullName(), saved.getEmail()),
                adminId, adminName, "ADMIN",
                saved.getId(), "USER", null
        );

        return UserResponse.fromEntity(saved);
    }

    /**
     * Admin resets a user's password.
     * Logs PASSWORD_CHANGED activity.
     */
    @Transactional
    public void resetUserPassword(Long userId, String newPassword,
                                   Long adminId, String adminName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        activityLogService.log(
                ActivityType.PASSWORD_CHANGED,
                String.format("Admin reset password for %s (%s)",
                        user.getFullName(), user.getEmail()),
                adminId, adminName, "ADMIN",
                userId, "USER", null
        );
    }

    /**
     * Soft-deletes a user (sets active = false).
     * Preferred over hard delete — preserves data integrity.
     * Logs USER_DEACTIVATED activity.
     */
    @Transactional
    public void deactivateUser(Long userId, Long adminId, String adminName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));

        user.setActive(false);
        userRepository.save(user);

        activityLogService.log(
                ActivityType.USER_DEACTIVATED,
                String.format("Admin deactivated user %s (%s)",
                        user.getFullName(), user.getEmail()),
                adminId, adminName, "ADMIN",
                userId, "USER", null
        );
    }

    /**
     * Reactivates a previously deactivated user.
     * Logs USER_REACTIVATED activity.
     */
    @Transactional
    public void reactivateUser(Long userId, Long adminId, String adminName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));

        user.setActive(true);
        userRepository.save(user);

        activityLogService.log(
                ActivityType.USER_REACTIVATED,
                String.format("Admin reactivated user %s (%s)",
                        user.getFullName(), user.getEmail()),
                adminId, adminName, "ADMIN",
                userId, "USER", null
        );
    }

    /**
     * Hard-deletes a user permanently.
     * Use with caution — prefer deactivateUser for most cases.
     * Logs USER_DELETED activity.
     */
    @Transactional
    public void hardDeleteUser(Long userId, Long adminId, String adminName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));

        String name  = user.getFullName();
        String email = user.getEmail();

        userRepository.deleteById(userId);

        activityLogService.log(
                ActivityType.USER_DELETED,
                String.format("Admin permanently deleted user %s (%s)", name, email),
                adminId, adminName, "ADMIN",
                userId, "USER", null
        );
    }

    /**
     * Assigns a new role to a user.
     * Logs ROLE_ASSIGNED activity.
     */
    @Transactional
    public UserResponse assignRole(Long userId, Role newRole,
                                    Long adminId, String adminName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));

        Role oldRole = user.getRole();
        user.setRole(newRole);
        User saved = userRepository.save(user);

        activityLogService.log(
                ActivityType.ROLE_ASSIGNED,
                String.format("Admin changed role of %s from %s to %s",
                        saved.getFullName(), oldRole.name(), newRole.name()),
                adminId, adminName, "ADMIN",
                userId, "USER",
                "oldRole=" + oldRole.name() + ",newRole=" + newRole.name()
        );

        return UserResponse.fromEntity(saved);
    }

    // ================================================================
    //  CLASS MANAGEMENT
    // ================================================================

    /**
     * Returns all active classes with their student counts.
     * Classes are derived from distinct student.className values.
     *
     * Note: assignment and note counts are not available here
     * (they live in separate services). The admin frontend should
     * call the Assignment and Notes services separately for those counts,
     * or an API Gateway can aggregate them later.
     */
    @Transactional(readOnly = true)
    public List<ClassOverviewResponse> getAllClasses() {
        List<String> classNames = userRepository.findDistinctClassNames();

        return classNames.stream()
                .map(className -> {
                    long studentCount = userRepository.countByClassNameAndRole(
                            className, Role.STUDENT);

                    // Get teacher IDs from users who have this class
                    // (teachers don't have className, so we derive from assignments)
                    // For now we return student count — teacher list comes from Assignment Service
                    return ClassOverviewResponse.builder()
                            .className(className)
                            .totalStudents(studentCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns details for a specific class.
     */
    @Transactional(readOnly = true)
    public ClassOverviewResponse getClassDetails(String className) {
        long studentCount = userRepository.countByClassNameAndRole(className, Role.STUDENT);

        if (studentCount == 0) {
            throw new ResourceNotFoundException("Class not found: " + className);
        }

        return ClassOverviewResponse.builder()
                .className(className)
                .totalStudents(studentCount)
                .build();
    }

    /**
     * Returns all students in a specific class.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getStudentsByClass(String className) {
        return userRepository.findByClassNameAndRole(className, Role.STUDENT)
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Returns all distinct academic years in the system.
     */
    @Transactional(readOnly = true)
    public List<String> getAcademicYears() {
        return userRepository.findDistinctAcademicYears();
    }

    /**
     * Looks up a user by email — used by the controller to resolve
     * the admin's identity from the JWT principal.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        return UserResponse.fromEntity(
                userRepository.findByEmail(email)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "User not found with email: " + email)));
    }
}
