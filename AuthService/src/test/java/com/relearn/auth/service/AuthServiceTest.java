package com.relearn.auth.service;

import com.relearn.auth.dto.AuthResponse;
import com.relearn.auth.dto.LoginRequest;
import com.relearn.auth.dto.RefreshTokenRequest;
import com.relearn.auth.entity.RefreshToken;
import com.relearn.auth.entity.User;
import com.relearn.auth.enums.Role;
import com.relearn.auth.exception.ResourceNotFoundException;
import com.relearn.auth.jwt.JwtUtils;
import com.relearn.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService unit tests")
class AuthServiceTest {

    @Mock UserRepository         userRepository;
    @Mock PasswordEncoder        passwordEncoder;
    @Mock JwtUtils               jwtUtils;
    @Mock AuthenticationManager  authenticationManager;
    @Mock RefreshTokenService    refreshTokenService;
    @Mock ActivityLogService     activityLogService;

    @InjectMocks AuthService authService;

    private User user;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .fullName("Alice Johnson")
                .email("alice@relearn.edu")
                .password("hashed")
                .role(Role.STUDENT)
                .className("Y1A")
                .academicYear("2024-2025")
                .active(true)
                .mustChangePassword(false)
                .build();

        refreshToken = RefreshToken.builder()
                .id(1L)
                .token("refresh-uuid-token")
                .user(user)
                .expiryDate(Instant.now().plusSeconds(604800))
                .build();
    }

    // ----------------------------------------------------------------
    //  login — happy path
    // ----------------------------------------------------------------

    @Test
    @DisplayName("login: records lastLoginAt on successful login")
    void login_recordsLastLoginAt() {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@relearn.edu");
        req.setPassword("password");

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("alice@relearn.edu")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(jwtUtils.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any())).thenReturn(refreshToken);

        authService.login(req);

        // lastLoginAt must be set
        assertThat(user.getLastLoginAt()).isNotNull();
        assertThat(user.getLastLoginAt()).isBeforeOrEqualTo(LocalDateTime.now());

        // user must be saved with the updated lastLoginAt
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("login: returns AuthResponse with mustChangePassword flag")
    void login_returnsMustChangePasswordFlag() {
        user.setMustChangePassword(true);

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@relearn.edu");
        req.setPassword("password");

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("alice@relearn.edu")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(jwtUtils.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any())).thenReturn(refreshToken);

        AuthResponse response = authService.login(req);

        assertThat(response.isMustChangePassword()).isTrue();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-uuid-token");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getRole()).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("login: activates account if inactive on first login")
    void login_activatesInactiveAccount() {
        user.setActive(false);

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@relearn.edu");
        req.setPassword("password");

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("alice@relearn.edu")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(jwtUtils.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any())).thenReturn(refreshToken);

        authService.login(req);

        assertThat(user.isActive()).isTrue();
    }

    @Test
    @DisplayName("login: throws when credentials are wrong")
    void login_throwsOnBadCredentials() {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@relearn.edu");
        req.setPassword("wrong");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login: throws ResourceNotFoundException when user missing from DB")
    void login_throwsWhenUserMissingFromDB() {
        LoginRequest req = new LoginRequest();
        req.setEmail("ghost@relearn.edu");
        req.setPassword("pass");

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("ghost@relearn.edu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ----------------------------------------------------------------
    //  refreshToken
    // ----------------------------------------------------------------

    @Test
    @DisplayName("refreshToken: returns new access token and same refresh token")
    void refreshToken_returnsNewAccessToken() {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("refresh-uuid-token");

        when(refreshTokenService.findByToken("refresh-uuid-token")).thenReturn(refreshToken);
        doNothing().when(refreshTokenService).verifyExpiration(refreshToken);
        when(jwtUtils.generateAccessToken(1L, "alice@relearn.edu", "STUDENT"))
                .thenReturn("new-access-token");

        AuthResponse response = authService.refreshToken(req);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-uuid-token");
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    // ----------------------------------------------------------------
    //  logout
    // ----------------------------------------------------------------

    @Test
    @DisplayName("logout: delegates to refreshTokenService")
    void logout_deletesRefreshToken() {
        when(userRepository.existsById(1L)).thenReturn(true);

        authService.logout(1L);

        verify(refreshTokenService).deleteByUserId(1L);
    }

    @Test
    @DisplayName("logout: silently ignores unknown userId")
    void logout_silentWhenUserNotFound() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatCode(() -> authService.logout(99L)).doesNotThrowAnyException();
        verify(refreshTokenService, never()).deleteByUserId(any());
    }
}
