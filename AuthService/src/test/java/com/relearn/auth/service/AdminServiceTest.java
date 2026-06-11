package com.relearn.auth.service;

import com.relearn.auth.dto.AdminCreateUserRequest;
import com.relearn.auth.dto.UserResponse;
import com.relearn.auth.entity.User;
import com.relearn.auth.enums.Role;
import com.relearn.auth.exception.ResourceNotFoundException;
import com.relearn.auth.repository.AcademicClassRepository;
import com.relearn.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService unit tests")
class AdminServiceTest {

    @Mock UserRepository          userRepository;
    @Mock AcademicClassRepository classRepository;
    @Mock ActivityLogService      activityLogService;
    @Mock PasswordEncoder         passwordEncoder;
    @Mock EmailService            emailService;

    @InjectMocks AdminService adminService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
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
    }

    // ----------------------------------------------------------------
    //  createUser — auto-generate password
    // ----------------------------------------------------------------

    @Test
    @DisplayName("createUser: auto-generates password when none provided")
    void createUser_autoGeneratesPassword_whenNotProvided() {
        AdminCreateUserRequest req = new AdminCreateUserRequest();
        req.setFullName("Bob Smith");
        req.setEmail("bob@relearn.edu");
        // password intentionally null
        req.setRole(Role.STUDENT);
        req.setClassName("Y1A");
        req.setAcademicYear("2024-2025");

        when(userRepository.existsByEmail("bob@relearn.edu")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        User saved = User.builder()
                .id(2L).fullName("Bob Smith").email("bob@relearn.edu")
                .password("hashed").role(Role.STUDENT)
                .className("Y1A").academicYear("2024-2025")
                .active(true).mustChangePassword(true).build();
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserResponse response = adminService.createUser(req, 99L, "Admin");

        // Password encoder must have been called with a non-blank generated password
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(passwordCaptor.capture());
        assertThat(passwordCaptor.getValue()).isNotBlank();
        assertThat(passwordCaptor.getValue().length()).isGreaterThanOrEqualTo(8);

        // Welcome email must be sent
        verify(emailService).sendWelcomeEmail(
                eq("bob@relearn.edu"), eq("Bob Smith"),
                eq("STUDENT"), anyString(),
                eq("Y1A"), eq("2024-2025")
        );

        // generatedPassword exposed in response
        assertThat(response.getGeneratedPassword()).isNotNull();
        // mustChangePassword is true for admin-created accounts
        assertThat(response.isMustChangePassword()).isTrue();
    }

    @Test
    @DisplayName("createUser: uses provided password when given")
    void createUser_usesProvidedPassword() {
        AdminCreateUserRequest req = new AdminCreateUserRequest();
        req.setFullName("Carol Doe");
        req.setEmail("carol@relearn.edu");
        req.setPassword("MySecure1!");
        req.setRole(Role.TEACHER);

        when(userRepository.existsByEmail("carol@relearn.edu")).thenReturn(false);
        when(passwordEncoder.encode("MySecure1!")).thenReturn("hashed");

        User saved = User.builder()
                .id(3L).fullName("Carol Doe").email("carol@relearn.edu")
                .password("hashed").role(Role.TEACHER)
                .active(true).mustChangePassword(true).build();
        when(userRepository.save(any(User.class))).thenReturn(saved);

        adminService.createUser(req, 99L, "Admin");

        // The provided password must be used (not a generated one)
        verify(passwordEncoder).encode("MySecure1!");
    }

    @Test
    @DisplayName("createUser: throws when email already registered")
    void createUser_throwsOnDuplicateEmail() {
        AdminCreateUserRequest req = new AdminCreateUserRequest();
        req.setFullName("Dave");
        req.setEmail("alice@relearn.edu");
        req.setRole(Role.STUDENT);

        when(userRepository.existsByEmail("alice@relearn.edu")).thenReturn(true);

        assertThatThrownBy(() -> adminService.createUser(req, 1L, "Admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    // ----------------------------------------------------------------
    //  getUserById
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getUserById: returns user when found")
    void getUserById_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserResponse response = adminService.getUserById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("alice@relearn.edu");
    }

    @Test
    @DisplayName("getUserById: throws when not found")
    void getUserById_throwsWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ----------------------------------------------------------------
    //  deactivateUser
    // ----------------------------------------------------------------

    @Test
    @DisplayName("deactivateUser: sets active=false")
    void deactivateUser_setsActiveFalse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any())).thenReturn(sampleUser);

        adminService.deactivateUser(1L, 99L, "Admin");

        assertThat(sampleUser.isActive()).isFalse();
        verify(userRepository).save(sampleUser);
    }

    // ----------------------------------------------------------------
    //  reactivateUser
    // ----------------------------------------------------------------

    @Test
    @DisplayName("reactivateUser: sets active=true")
    void reactivateUser_setsActiveTrue() {
        sampleUser.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any())).thenReturn(sampleUser);

        adminService.reactivateUser(1L, 99L, "Admin");

        assertThat(sampleUser.isActive()).isTrue();
    }
}
