package com.relearn.auth.entity;

import com.relearn.auth.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a user in the Relearn platform.
 * Passwords are stored encrypted (BCrypt).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Full display name of the user */
    @Column(nullable = false)
    private String fullName;

    /** Email is used as the unique login identifier */
    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt-encrypted password — never stored in plain text */
    @Column(nullable = false)
    private String password;

    /** Role determines what the user can access */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * The class the student currently belongs to (e.g. "Y1A", "Y2C").
     * Only relevant for STUDENT role. Null for TEACHER/ADMIN.
     */
    @Column
    private String className;

    /**
     * The current academic year (e.g. "2024-2025").
     * Used to scope dashboard data and history queries.
     */
    @Column
    private String academicYear;

    /**
     * Whether this account is active.
     * Soft-delete: set to false instead of deleting the record.
     * Defaults to true on creation.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Automatically set when the record is first created */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Automatically updated on every save */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Timestamp of the user's most recent successful login.
     * Null if the user has never logged in.
     * Used by the reminder scheduler to detect users who haven't logged in yet.
     */
    @Column
    private LocalDateTime lastLoginAt;

    /**
     * When true, the user must change their password on next login.
     * Set to true when admin creates the account (system-generated password).
     * Set to false after the user changes their password.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean mustChangePassword = false;
}
