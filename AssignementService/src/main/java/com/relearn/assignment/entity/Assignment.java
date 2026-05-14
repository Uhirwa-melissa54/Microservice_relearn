package com.relearn.assignment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents an assignment created by a teacher on the Relearn platform.
 *
 * Assignments are scoped by class and course so students only see
 * what is relevant to them.
 *
 * Note: teacherId is stored as a plain Long for now.
 * When microservice communication is added later, this will be used
 * to call the Auth Service to resolve the full teacher profile.
 */
@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Short title of the assignment (e.g. "Chapter 5 Exercises") */
    @Column(nullable = false)
    private String title;

    /** Detailed instructions or description of what students need to do */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** The date and time by which students must submit */
    @Column(nullable = false)
    private LocalDateTime deadline;

    /** The class this assignment is for (e.g. "Grade 10A") */
    @Column(nullable = false)
    private String className;

    /** The course/subject this assignment belongs to (e.g. "Mathematics") */
    @Column(nullable = false)
    private String courseName;

    /**
     * Academic year this assignment belongs to (e.g. "2024-2025").
     * Used for history queries.
     */
    @Column
    private String academicYear;

    /**
     * ID of the teacher who created this assignment.
     * References a user in the Auth Service (resolved later via inter-service call).
     */
    @Column(nullable = false)
    private Long teacherId;

    /**
     * Optional URL to a file attached to this assignment (PDF, DOCX, etc.).
     * Students can download this to read the full assignment instructions.
     */
    @Column
    private String fileUrl;

    /** Automatically set when the assignment is first saved */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Automatically updated every time the assignment is modified */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
