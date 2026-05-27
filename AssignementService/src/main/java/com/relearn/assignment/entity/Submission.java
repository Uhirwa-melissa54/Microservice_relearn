package com.relearn.assignment.entity;

import com.relearn.assignment.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a student's submission for a specific assignment.
 *
 * Each submission is linked to one assignment and one student.
 * A student can only submit once per assignment (enforced by the unique constraint).
 * Teachers can grade submissions by setting score, feedback, and status = GRADED.
 */
@Entity
@Table(
    name = "submissions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_submission_assignment_student",
        columnNames = {"assignment_id", "student_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The assignment this submission belongs to */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    /** ID of the student who submitted (references Auth Service) */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** The student's written answer (optional if file is provided) */
    @Column(columnDefinition = "TEXT")
    private String submissionText;

    /** URL to the submitted file (optional if text is provided) */
    @Column
    private String fileUrl;

    /** Automatically set to the moment the student submits */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    /** Updated whenever the submission is modified (resubmit or graded) */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Current status: PENDING → GRADED (or LATE if submitted after deadline).
     * MISSING is a virtual status computed for students who never submitted.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.PENDING;

    // ----------------------------------------------------------------
    //  Grading fields — set by teacher
    // ----------------------------------------------------------------

    /**
     * Numeric score assigned by the teacher (e.g. 85.5 out of 100).
     * Null until graded.
     */
    @Column
    private Double score;

    /**
     * Maximum possible score for this assignment.
     * Copied from the assignment when grading so it's self-contained.
     */
    @Column
    private Double maxScore;

    /**
     * Teacher's written feedback/comments on the submission.
     * Null until graded.
     */
    @Column(columnDefinition = "TEXT")
    private String feedback;

    /**
     * When the teacher graded this submission.
     * Null until graded.
     */
    @Column
    private LocalDateTime gradedAt;

    /**
     * ID of the teacher who graded this submission.
     * References Auth Service.
     */
    @Column
    private Long gradedBy;
}
