package com.relearn.auth.service;

import com.relearn.auth.dto.*;
import com.relearn.auth.entity.RefreshToken;
import com.relearn.auth.entity.User;
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
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    // ----------------------------------------------------------------
    //  Register
    // ----------------------------------------------------------------

    /**
     * Registers a new user.
     *
     * Steps:
     *  1. Check email is not already taken
     *  2. Encrypt the password with BCrypt
     *  3. Save the user to the database
     *  4. Return a JWT access token + refresh token immediately
     *
     * @param request registration data from the client
     * @return AuthResponse with access token, refresh token, and user info
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Prevent duplicate accounts
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Email is already registered: " + request.getEmail());
        }

        // Build and save the new user
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt hash
                .role(request.getRole())
                .className(request.getClassName())
                .academicYear(request.getAcademicYear())
                .build();

        User savedUser = userRepository.save(user);

        // Generate tokens for the newly registered user
        return buildAuthResponse(savedUser);
    }

    // ----------------------------------------------------------------
    //  Login
    // ----------------------------------------------------------------

    /**
     * Authenticates a user with email and password.
     *
     * Spring Security's AuthenticationManager handles the actual
     * credential verification (loads user from DB, checks BCrypt hash).
     * If credentials are wrong, it throws BadCredentialsException automatically.
     *
     * @param request login credentials from the client
     * @return AuthResponse with access token, refresh token, and user info
     */
    public AuthResponse login(LoginRequest request) {
        // This line does the actual authentication — throws if credentials are wrong
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Load the full user entity to include in the response
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + request.getEmail()));

        return buildAuthResponse(user);
    }

    // ----------------------------------------------------------------
    //  Refresh Token
    // ----------------------------------------------------------------

    /**
     * Issues a new access token using a valid refresh token.
     *
     * The refresh token is NOT rotated here (same refresh token is reused
     * until it expires). You can change this to rotate if needed.
     *
     * @param request contains the refresh token string
     * @return AuthResponse with a new access token (same refresh token)
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        // Find the refresh token in the database
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken());

        // Check it hasn't expired
        refreshTokenService.verifyExpiration(refreshToken);

        // Get the associated user
        User user = refreshToken.getUser();

        // Generate a new access token
        String newAccessToken = jwtUtils.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken()) // same refresh token
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    // ----------------------------------------------------------------
    //  Logout
    // ----------------------------------------------------------------

    /**
     * Invalidates the user's refresh token (logout).
     * The access token will naturally expire on its own (15 min).
     *
     * @param userId the ID of the user logging out
     */
    @Transactional
    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));
        refreshTokenService.deleteByUser(user);
    }

    // ----------------------------------------------------------------
    //  Internal Helper
    // ----------------------------------------------------------------

    /**
     * Builds the full AuthResponse for a user.
     * Creates a fresh refresh token (replaces any existing one).
     */
    private AuthResponse buildAuthResponse(User user) {
        // Generate short-lived access token (15 min)
        String accessToken = jwtUtils.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        // Generate long-lived refresh token (7 days), stored in DB
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
