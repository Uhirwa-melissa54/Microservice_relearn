package com.relearn.auth.repository;

import com.relearn.auth.entity.AcademicClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for AcademicClass entities.
 *
 * AcademicClass is the authoritative source of truth for which classes exist.
 * Admin creates/edits/deletes classes here. Students and teachers reference
 * className strings that must match entries in this table.
 */
@Repository
public interface AcademicClassRepository extends JpaRepository<AcademicClass, Long> {

    /** Find a class by its unique name (e.g. "Y1A") */
    Optional<AcademicClass> findByClassName(String className);

    /** Check if a class name is already registered */
    boolean existsByClassName(String className);

    /** All active classes */
    List<AcademicClass> findByActiveTrue();

    /** All classes assigned to a specific teacher */
    List<AcademicClass> findByTeacherId(Long teacherId);

    /** All active classes assigned to a specific teacher */
    List<AcademicClass> findByTeacherIdAndActiveTrue(Long teacherId);

    /** All classes for a specific academic year */
    List<AcademicClass> findByAcademicYear(String academicYear);
}
