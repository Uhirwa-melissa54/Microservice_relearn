package com.relearn.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO returned after a successful login or token refresh.
 * Contains both the short-lived access token and the long-lived refresh token.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private final String tokenType = "Bearer";

    private Long   userId;
    private String email;
    private String role;
    private String fullName;
    private String className;
    private String academicYear;

    /**
     * True when the user must change their password on next login.
     * Set to true for admin-created accounts with auto-generated passwords.
     * Frontend should redirect to a change-password screen when this is true.
     */
    private boolean mustChangePassword;
}
