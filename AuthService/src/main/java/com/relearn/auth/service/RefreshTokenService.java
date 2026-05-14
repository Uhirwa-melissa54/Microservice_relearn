package com.relearn.auth.service;

import com.relearn.auth.entity.RefreshToken;
import com.relearn.auth.entity.User;
import com.relearn.auth.exception.TokenRefreshException;
import com.relearn.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Handles all refresh token operations:
 *  - Creating a new refresh token for a user
 *  - Verifying a refresh token hasn't expired
 *  - Deleting tokens on logout
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Creates a new refresh token for the given user.
     * If the user already has a refresh token, it is replaced (one token per user).
     *
     * @param user the user to create the token for
     * @return the saved RefreshToken entity
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Delete any existing refresh token for this user first
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                // UUID gives us a secure, random, unique token string
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpiration))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Finds a refresh token by its string value.
     *
     * @param token the raw token string from the client
     * @return the RefreshToken entity
     * @throws TokenRefreshException if the token doesn't exist
     */
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenRefreshException(token, "Refresh token not found in database"));
    }

    /**
     * Checks if the refresh token has expired.
     * If expired, deletes it from the database and throws an exception.
     *
     * @param token the RefreshToken entity to verify
     * @return the same token if still valid
     * @throws TokenRefreshException if the token has expired
     */
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            // Clean up the expired token
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(
                    token.getToken(),
                    "Refresh token has expired. Please log in again."
            );
        }
        return token;
    }

    /**
     * Deletes all refresh tokens for a user (used on logout).
     *
     * @param user the user whose tokens should be invalidated
     */
    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
