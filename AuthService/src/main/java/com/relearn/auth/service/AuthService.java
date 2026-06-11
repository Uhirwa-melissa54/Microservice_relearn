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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Core authentication service.
 * Handles registration, login, token refresh, and logout.
 *
 * On login:
 *  - Records lastLoginAt timestamp (stops reminder emails)
 *  - Activates account if it was pending first login
 *  - Passes mustChangePassword flag to the frontend
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtUtils              jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService   refreshTokenService;
    private final ActivityLogService    activityLogService;

    // ----------------------------------------------------------------
    //  Register (public self-registration)
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
                .mustChangePassword(false)
                .build();

        User saved = userRepository.save(user);

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

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Spring Security verifies credentials — throws BadCredentialsException if wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + request.getEmail()));

        // Record the login time — this is what stops reminder emails
        user.setLastLoginAt(LocalDateTime.now());

        // Ensure account is active (covers edge case where admin created
        // an inactive account that was since activated by login)
        if (!user.isActive()) {
            user.setActive(true);
        }

        userRepository.save(user);

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
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }

    // ----------------------------------------------------------------
    //  Logout
    // ----------------------------------------------------------------

    @Transactional
    public void logout(Long userId) {
        if (!userRepository.existsById(userId)) return;
        refreshTokenService.deleteByUserId(userId);
    }

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
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }
}
