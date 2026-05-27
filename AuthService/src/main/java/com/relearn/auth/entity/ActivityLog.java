package com.relearn.auth.entity;

import com.relearn.auth.enums.ActivityType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks system-wide activity events for the admin dashboard.
 *
 * Events are logged whenever significant actions occur:
 * user created, class created, note uploaded, assignment created, etc.
 *
 * This is a write-once, read-many table — events are never updated or deleted.
 */
@Entity
@Table(name = "activity_logs",
       indexes = {
           @Index(name = "idx_activity_created_at", columnList = "created_at"),
           @Index(name = "idx_activity_type",       columnList = "activity_type"),
           @Index(name = "idx_activity_actor_id",   columnList = "actor_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The type of activity that occurred.
     * Used for filtering and categorization.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

    /**
     * Human-readable description of the event.
     * Example: "Teacher John Doe uploaded a note for Y1A - Mathematics"
     */
    @Column(nullable = false)
    private String description;

    /**
     * ID of the user who performed the action.
     * Null for system-generated events.
     */
    @Column(name = "actor_id")
    private Long actorId;

    /**
     * Name of the user who performed the action (denormalized for display).
     * Stored here so we don't need a join to show activity feeds.
     */
    @Column(name = "actor_name")
    private String actorName;

    /**
     * The role of the actor (ADMIN, TEACHER, STUDENT).
     */
    @Column(name = "actor_role")
    private String actorRole;

    /**
     * Optional: ID of the entity this event relates to.
     * e.g. the note ID, assignment ID, or user ID that was created/modified.
     */
    @Column(name = "entity_id")
    private Long entityId;

    /**
     * Optional: type of entity (e.g. "NOTE", "ASSIGNMENT", "USER", "CLASS").
     */
    @Column(name = "entity_type")
    private String entityType;

    /**
     * Optional: extra context (e.g. class name, course name).
     */
    @Column(name = "metadata")
    private String metadata;

    /** Automatically set when the log entry is created */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
