package com.relearn.assignment.repository;

import com.relearn.assignment.entity.Submission;
import com.relearn.assignment.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for Submission entities.
 */
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    /**
     * Find all submissions for a specific assignment.
     * Teachers use this to see who has submitted and review their work.
     *
     * @param assignmentId the ID of the assignment
     */
    List<Submission> findByAssignmentId(Long assignmentId);

    /**
     * Find all submissions made by a specific student.
     * Students use this to track their own submission history.
     *
     * @param studentId the ID of the student
     */
    List<Submission> findByStudentId(Long studentId);

    /**
     * Find a specific student's submission for a specific assignment.
     * Used to check if a student has already submitted before allowing a new one.
     *
     * @param assignmentId the ID of the assignment
     * @param studentId    the ID of the student
     */
    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    /** Count submissions for a specific assignment */
    long countByAssignmentId(Long assignmentId);

    /** Count submissions by a student with a specific status */
    long countByStudentIdAndStatus(Long studentId, SubmissionStatus status);

    /** Count all submissions by a student */
    long countByStudentId(Long studentId);
}
