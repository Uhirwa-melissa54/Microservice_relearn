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

    /** All distinct class names that have students */
    @Query("SELECT DISTINCT u.className FROM User u WHERE u.role = 'STUDENT' AND u.className IS NOT NULL")
    List<String> findDistinctClassNames();

    /** All distinct academic years */
    @Query("SELECT DISTINCT u.academicYear FROM User u WHERE u.academicYear IS NOT NULL ORDER BY u.academicYear DESC")
    List<String> findDistinctAcademicYears();

    // ----------------------------------------------------------------
    //  Paginated search — admin user management
    // ----------------------------------------------------------------

    /**
     * Search all users by name or email (case-insensitive), paginated.
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);

    /**
     * Search users filtered by role, paginated.
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND " +
           "(:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsersByRole(@Param("role") Role role,
                                  @Param("search") String search,
                                  Pageable pageable);

    /**
     * Search users filtered by role and active status, paginated.
     */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.active = :active AND " +
           "(:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsersByRoleAndStatus(@Param("role") Role role,
                                           @Param("active") boolean active,
                                           @Param("search") String search,
                                           Pageable pageable);

    // ----------------------------------------------------------------
    //  Activity / stats queries
    // ----------------------------------------------------------------

    /** Count users registered since a given time */
    long countByCreatedAtAfter(LocalDateTime since);

    /** Count active users */
    long countByActive(boolean active);
}
