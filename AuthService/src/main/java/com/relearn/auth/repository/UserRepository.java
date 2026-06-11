package com.relearn.auth.repository;

import com.relearn.auth.entity.User;
import com.relearn.auth.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for User entities.
 * Supports pagination, search, and role-based filtering for admin operations.
 *
 * NOTE on search queries:
 *   JPQL ':param IS NULL' is unreliable with PostgreSQL/Hibernate when the
 *   Java value is actually null. We use a workaround: pass '' (empty string)
 *   when no search term is provided, and match everything when search is blank.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // ----------------------------------------------------------------
    //  Role-based queries
    // ----------------------------------------------------------------

    List<User> findByRole(Role role);
    long countByRole(Role role);
    long countByRoleAndActive(Role role, boolean active);

    // ----------------------------------------------------------------
    //  Class-based queries
    // ----------------------------------------------------------------

    List<User> findByClassNameAndRole(String className, Role role);
    long countByClassNameAndRole(String className, Role role);

    @Query("SELECT DISTINCT u.className FROM User u WHERE u.role = 'STUDENT' AND u.className IS NOT NULL")
    List<String> findDistinctClassNames();

    @Query("SELECT DISTINCT u.className FROM User u WHERE u.role = 'STUDENT' AND u.active = true AND u.className IS NOT NULL")
    List<String> findDistinctActiveClassNames();

    @Query("SELECT DISTINCT u.academicYear FROM User u WHERE u.academicYear IS NOT NULL ORDER BY u.academicYear DESC")
    List<String> findDistinctAcademicYears();

    // ----------------------------------------------------------------
    //  Paginated search — admin user management
    //
    //  The trick: pass '' when search is null/blank.
    //  The query returns all rows when :search = '' because
    //  LIKE '%' matches everything.
    // ----------------------------------------------------------------

    /**
     * Search ALL users by name or email (case-insensitive), paginated.
     * Pass search='' to return all users.
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:search = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);

    /**
     * Search users filtered by role, paginated.
     * Pass search='' to return all users with that role.
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND " +
           "(:search = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsersByRole(@Param("role") Role role,
                                  @Param("search") String search,
                                  Pageable pageable);

    /**
     * Search users filtered by role and active status, paginated.
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.active = :active AND " +
           "(:search = '' OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsersByRoleAndStatus(@Param("role") Role role,
                                           @Param("active") boolean active,
                                           @Param("search") String search,
                                           Pageable pageable);

    // ----------------------------------------------------------------
    //  Activity / stats queries
    // ----------------------------------------------------------------

    long countByCreatedAtAfter(LocalDateTime since);
    long countByActive(boolean active);

    // ----------------------------------------------------------------
    //  Login reminder query
    // ----------------------------------------------------------------

    /**
     * Finds active users who have NEVER logged in and whose account
     * was created before the given cutoff time.
     * Used by LoginReminderScheduler to send reminder emails.
     */
    List<User> findByActiveAndLastLoginAtIsNullAndCreatedAtBefore(
            boolean active, LocalDateTime createdBefore);
}
