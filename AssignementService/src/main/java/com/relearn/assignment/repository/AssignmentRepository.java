package com.relearn.assignment.repository;

import com.relearn.assignment.entity.Assignment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data access layer for Assignment entities.
 */
@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByClassName(String className);

    List<Assignment> findByCourseName(String courseName);

    List<Assignment> findByTeacherId(Long teacherId);

    List<Assignment> findByClassNameAndCourseName(String className, String courseName);

    List<Assignment> findByClassNameAndAcademicYear(String className, String academicYear);

    /** Recent assignments for a class, newest first — used for dashboard */
    List<Assignment> findByClassNameOrderByCreatedAtDesc(String className, Pageable pageable);

    /** Overdue assignments: deadline is in the past */
    List<Assignment> findByClassNameAndDeadlineBefore(String className, LocalDateTime now);

    /** Active assignments: deadline is in the future */
    List<Assignment> findByClassNameAndDeadlineAfter(String className, LocalDateTime now);

    /** Count assignments for a class in a given academic year */
    long countByClassNameAndAcademicYear(String className, String academicYear);

    /** Count all assignments for a class */
    long countByClassName(String className);
}
