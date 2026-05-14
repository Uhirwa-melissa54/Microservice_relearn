package com.relearn.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Stores refresh tokens in the database.
 * Each user can have one active refresh token at a time.
 * When a new refresh token is issued, the old one is replaced.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The actual token string (UUID-based, stored as-is) */
    @Column(nullable = false, unique = true)
    private String token;

    /** The user this refresh token belongs to */
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    /** When this token expires — checked on every refresh request */
    @Column(nullable = false)
    private Instant expiryDate;
}
