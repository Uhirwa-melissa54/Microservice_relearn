package com.relearn.assignment.repository;

import com.relearn.assignment.entity.Assignment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data access layer for Assignment entities.
 */
@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    // ----------------------------------------------------------------
    //  Basic queries
    // ----------------------------------------------------------------

    List<Assignment> findByClassName(String className);
    List<Assignment> findByCourseName(String courseName);
    List<Assignment> findByTeacherId(Long teacherId);
    List<Assignment> findByClassNameAndCourseName(String className, String courseName);
    List<Assignment> findByClassNameAndAcademicYear(String className, String academicYear);
    List<Assignment> findByClassNameOrderByCreatedAtDesc(String className, Pageable pageable);
    List<Assignment> findByClassNameAndDeadlineBefore(String className, LocalDateTime now);
    List<Assignment> findByClassNameAndDeadlineAfter(String className, LocalDateTime now);

    // ----------------------------------------------------------------
    //  Teacher-scoped queries
    // ----------------------------------------------------------------

    List<Assignment> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    List<Assignment> findByTeacherIdAndClassNameAndCourseName(
            Long teacherId, String className, String courseName);

    List<Assignment> findByTeacherIdAndDeadlineAfter(Long teacherId, LocalDateTime now);

    List<Assignment> findByTeacherIdAndDeadlineBefore(Long teacherId, LocalDateTime now);

    List<Assignment> findByTeacherIdOrderByCreatedAtDesc(Long teacherId, Pageable pageable);

    @Query("SELECT DISTINCT a.className, a.courseName FROM Assignment a WHERE a.teacherId = :teacherId")
    List<Object[]> findDistinctClassCourseByTeacherId(Long teacherId);

    long countByTeacherId(Long teacherId);

    long countByTeacherIdAndClassNameAndCourseName(
            Long teacherId, String className, String courseName);

    long countByTeacherIdAndClassNameAndCourseNameAndDeadlineAfter(
            Long teacherId, String className, String courseName, LocalDateTime now);

    long countByTeacherIdAndClassNameAndCourseNameAndDeadlineBefore(
            Long teacherId, String className, String courseName, LocalDateTime now);

    // ----------------------------------------------------------------
    //  Student-scoped queries
    // ----------------------------------------------------------------

    long countByClassNameAndAcademicYear(String className, String academicYear);

    /** Count all assignments for a class */
    long countByClassName(String className);

    // ----------------------------------------------------------------
    //  Admin-scoped queries
    // ----------------------------------------------------------------

    /** Count active assignments system-wide (deadline in future) */
    long countByDeadlineAfter(LocalDateTime now);

    /** Count overdue assignments system-wide (deadline in past) */
    long countByDeadlineBefore(LocalDateTime now);

    /** All distinct class names that have assignments */
    @Query("SELECT DISTINCT a.className FROM Assignment a WHERE a.className IS NOT NULL")
    List<String> findDistinctClassNames();

    /** Recent assignments system-wide, newest first */
    List<Assignment> findTop10ByOrderByCreatedAtDesc();
}
