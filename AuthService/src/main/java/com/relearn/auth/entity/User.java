package com.relearn.auth.entity;

import com.relearn.auth.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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

    /** Automatically set when the record is first created */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
