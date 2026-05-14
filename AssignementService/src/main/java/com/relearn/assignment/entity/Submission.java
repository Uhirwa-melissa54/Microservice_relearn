package com.relearn.assignment.entity;

import com.relearn.assignment.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a student's submission for a specific assignment.
 *
 * Each submission is linked to one assignment and one student.
 * A student can only submit once per assignment (enforced by the unique constraint).
 *
 * Note: studentId is stored as a plain Long for now.
 * When microservice communication is added later, this will be used
 * to call the Auth Service to resolve the full student profile.
 */
@Entity
@Table(
    name = "submissions",
    // Prevent a student from submitting the same assignment twice
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

    /**
     * The assignment this submission belongs to.
     * Many submissions can belong to one assignment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    /**
     * ID of the student who submitted.
     * References a user in the Auth Service (resolved later via inter-service call).
     */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /** The student's written answer or response text (optional if file is provided) */
    @Column(columnDefinition = "TEXT")
    private String submissionText;

    /**
     * URL or path to the submitted file (PDF, DOCX, etc.).
     * Optional — student can submit text only, file only, or both.
     */
    @Column
    private String fileUrl;

    /** Automatically set to the moment the student submits */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    /**
     * Current status of the submission.
     * Defaults to PENDING when first submitted.
     * The service automatically sets it to LATE if submitted after the deadline.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.PENDING;
}
