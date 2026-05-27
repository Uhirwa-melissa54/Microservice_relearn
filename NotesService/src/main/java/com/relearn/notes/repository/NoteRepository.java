package com.relearn.notes.repository;

import com.relearn.notes.entity.Note;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data access layer for Note entities.
 */
@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByClassName(String className);

    List<Note> findByCourseName(String courseName);

    List<Note> findByAcademicYear(String academicYear);

    List<Note> findByTeacherId(Long teacherId);

    List<Note> findByClassNameAndCourseName(String className, String courseName);

    List<Note> findByClassNameAndAcademicYear(String className, String academicYear);

    /**
     * Returns the most recently uploaded notes for a class, ordered by creation date.
     * Used for the dashboard "recent notes" section.
     */
    List<Note> findByClassNameOrderByCreatedAtDesc(String className, Pageable pageable);

    /**
     * Returns all distinct course names that have notes for a given class and academic year.
     * Used to build the course list on the student dashboard.
     */
    @Query("SELECT DISTINCT n.courseName FROM Note n WHERE n.className = :className AND n.academicYear = :academicYear")
    List<String> findDistinctCourseNamesByClassNameAndAcademicYear(String className, String academicYear);

    /** Count notes for a class in a given academic year */
    long countByClassNameAndAcademicYear(String className, String academicYear);

    /** Count notes for a specific course in a class */
    long countByClassNameAndCourseName(String className, String courseName);

    // ----------------------------------------------------------------
    //  Teacher-scoped queries
    // ----------------------------------------------------------------

    /** All notes by a teacher, newest first */
    List<Note> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    /** Teacher's notes for a specific class+course */
    List<Note> findByTeacherIdAndClassNameAndCourseName(
            Long teacherId, String className, String courseName);

    /** Teacher's notes for a specific class */
    List<Note> findByTeacherIdAndClassName(Long teacherId, String className);

    /** Count notes by teacher */
    long countByTeacherId(Long teacherId);

    /** Count notes by teacher for a specific class+course */
    long countByTeacherIdAndClassNameAndCourseName(
            Long teacherId, String className, String courseName);

    /** All distinct class+course combinations for a teacher */
    @Query("SELECT DISTINCT n.className, n.courseName FROM Note n WHERE n.teacherId = :teacherId")
    List<Object[]> findDistinctClassCourseByTeacherId(Long teacherId);

    // ----------------------------------------------------------------
    //  Admin-scoped queries
    // ----------------------------------------------------------------

    /** All distinct class names that have notes */
    @Query("SELECT DISTINCT n.className FROM Note n WHERE n.className IS NOT NULL")
    List<String> findDistinctClassNames();

    /** All distinct course names that have notes */
    @Query("SELECT DISTINCT n.courseName FROM Note n WHERE n.courseName IS NOT NULL")
    List<String> findDistinctCourseNames();

    /** All distinct teacher IDs who have uploaded notes */
    @Query("SELECT DISTINCT n.teacherId FROM Note n WHERE n.teacherId IS NOT NULL")
    List<Long> findDistinctTeacherIds();

    /** Recent notes system-wide, newest first */
    List<Note> findTop10ByOrderByCreatedAtDesc();
}
