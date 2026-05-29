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

    /** Short-lived JWT — sent in Authorization header for every API call */
    private String accessToken;

    /** Long-lived token — used only to get a new access token */
    private String refreshToken;

    /** Token type, always "Bearer" */
    private final String tokenType = "Bearer";

    /** The authenticated user's basic info */
    private Long userId;
    private String email;
    private String role;
    private String fullName;
    private String className;
    private String academicYear;
}
