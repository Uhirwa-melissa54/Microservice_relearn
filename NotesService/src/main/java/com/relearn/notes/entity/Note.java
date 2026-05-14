package com.relearn.notes.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a note uploaded by a teacher on the Relearn platform.
 *
 * Notes are scoped by class, course, and academic year so students
 * can filter and find relevant material easily.
 *
 * Note: teacherId is stored as a plain Long for now.
 * When microservice communication is added later, this will be
 * used to call the Auth Service to resolve the full teacher profile.
 */
@Entity
@Table(name = "notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Short title describing the note (e.g. "Chapter 3 - Algebra") */
    @Column(nullable = false)
    private String title;

    /** Optional longer description or summary of the note content */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * URL or path to the uploaded file (PDF, DOCX, etc.).
     * In a full implementation this would point to cloud storage (S3, GCS, etc.)
     */
    @Column
    private String fileUrl;

    /** The class this note belongs to (e.g. "Grade 10A") */
    @Column(nullable = false)
    private String className;

    /** The course/subject this note belongs to (e.g. "Mathematics") */
    @Column(nullable = false)
    private String courseName;

    /**
     * Academic year the note belongs to (e.g. "2024-2025").
     * Allows students to browse notes from previous years.
     */
    @Column(nullable = false)
    private String academicYear;

    /**
     * ID of the teacher who uploaded this note.
     * References a user in the Auth Service (resolved later via inter-service call).
     */
    @Column(nullable = false)
    private Long teacherId;

    /** Automatically set when the note is first saved */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Automatically updated every time the note is modified */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
