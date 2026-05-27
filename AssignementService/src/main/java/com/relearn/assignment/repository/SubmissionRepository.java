package com.relearn.assignment.repository;

import com.relearn.assignment.entity.Submission;
import com.relearn.assignment.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for Submission entities.
 */
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByAssignmentId(Long assignmentId);
    List<Submission> findByStudentId(Long studentId);
    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    /** Filter submissions for an assignment by status */
    List<Submission> findByAssignmentIdAndStatus(Long assignmentId, SubmissionStatus status);

    /** All submissions for assignments created by a specific teacher */
    @Query("SELECT s FROM Submission s WHERE s.assignment.teacherId = :teacherId")
    List<Submission> findByTeacherId(Long teacherId);

    /** Pending review submissions for a teacher (PENDING + LATE = not yet graded) */
    @Query("SELECT s FROM Submission s WHERE s.assignment.teacherId = :teacherId " +
           "AND s.status IN ('PENDING', 'LATE')")
    List<Submission> findPendingReviewByTeacherId(Long teacherId);

    /** Count pending review submissions for a teacher */
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.assignment.teacherId = :teacherId " +
           "AND s.status IN ('PENDING', 'LATE')")
    long countPendingReviewByTeacherId(Long teacherId);

    /** Count pending review submissions for a specific assignment */
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.assignment.id = :assignmentId " +
           "AND s.status IN ('PENDING', 'LATE')")
    long countPendingReviewByAssignmentId(Long assignmentId);

    /** Count graded submissions for a specific assignment */
    long countByAssignmentIdAndStatus(Long assignmentId, SubmissionStatus status);

    /** Count all submissions for an assignment */
    long countByAssignmentId(Long assignmentId);

    /** Count submissions by a student with a specific status */
    long countByStudentIdAndStatus(Long studentId, SubmissionStatus status);

    /** Count all submissions by a student */
    long countByStudentId(Long studentId);

    /** Pending review for a specific class+course teacher combo */
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.assignment.teacherId = :teacherId " +
           "AND s.assignment.className = :className AND s.assignment.courseName = :courseName " +
           "AND s.status IN ('PENDING', 'LATE')")
    long countPendingReviewByTeacherAndClassAndCourse(
            Long teacherId, String className, String courseName);

    // ----------------------------------------------------------------
    //  Admin-scoped queries
    // ----------------------------------------------------------------

    /** Total submissions system-wide by status */
    long countByStatus(SubmissionStatus status);

    /** Total pending reviews system-wide */
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.status IN ('PENDING', 'LATE')")
    long countAllPendingReviews();

    /** Recent submissions system-wide, newest first */
    List<Submission> findTop10ByOrderBySubmittedAtDesc();
}
