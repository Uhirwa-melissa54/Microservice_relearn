package com.relearn.auth.service;

import com.relearn.auth.dto.*;
import com.relearn.auth.entity.RefreshToken;
import com.relearn.auth.entity.User;
import com.relearn.auth.enums.ActivityType;
import com.relearn.auth.exception.ResourceNotFoundException;
import com.relearn.auth.jwt.JwtUtils;
import com.relearn.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core authentication service.
 * Handles user registration, login, token refresh, and logout.
 * All significant events are logged to the activity log.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository       userRepository;
    private final PasswordEncoder      passwordEncoder;
    private final JwtUtils             jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService  refreshTokenService;
    private final ActivityLogService   activityLogService;

    // ----------------------------------------------------------------
    //  Register
    // ----------------------------------------------------------------

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Email is already registered: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .className(request.getClassName())
                .academicYear(request.getAcademicYear())
                .active(true)
                .build();

        User saved = userRepository.save(user);

        // Log the registration event
        activityLogService.log(
                ActivityType.USER_REGISTERED,
                String.format("New %s registered: %s (%s)",
                        saved.getRole().name().toLowerCase(),
                        saved.getFullName(), saved.getEmail()),
                saved.getId(), saved.getFullName(), saved.getRole().name(),
                saved.getId(), "USER", "role=" + saved.getRole().name()
        );

        return buildAuthResponse(saved);
    }

    // ----------------------------------------------------------------
    //  Login
    // ----------------------------------------------------------------

    public AuthResponse login(LoginRequest request) {
        // Spring Security verifies credentials — throws BadCredentialsException if wrong
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + request.getEmail()));

        return buildAuthResponse(user);
    }

    // ----------------------------------------------------------------
    //  Refresh Token
    // ----------------------------------------------------------------

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken());
        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();

        String newAccessToken = jwtUtils.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken())
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .className(user.getClassName())
                .academicYear(user.getAcademicYear())
                .build();
    }

    // ----------------------------------------------------------------
    //  Logout
    // ----------------------------------------------------------------

  /**
     * Invalidates the refresh token for the given user (admin, teacher, or student).
     * Removes the row from the database so it cannot be used to obtain new access tokens.
     */
    @Transactional
    public void logout(Long userId) {
        if (!userRepository.existsById(userId)) {
            return;
        }
        refreshTokenService.deleteByUserId(userId);
    }

    /**
     * Invalidates the refresh token string sent by the client on logout.
     * Works for any role; preferred over userId because it does not depend on localStorage user id.
     */
    @Transactional
    public void logoutByRefreshToken(String refreshToken) {
        refreshTokenService.deleteByToken(refreshToken);
    }

    // ----------------------------------------------------------------
    //  Internal helper
    // ----------------------------------------------------------------

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtils.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .className(user.getClassName())
                .academicYear(user.getAcademicYear())
                .build();
    }
}
