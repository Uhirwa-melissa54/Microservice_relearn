package com.relearn.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relearn.auth.dto.AuthResponse;
import com.relearn.auth.dto.LoginRequest;
import com.relearn.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController integration tests")
class AuthControllerTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    @MockBean AuthService    authService;

    // ----------------------------------------------------------------
    //  POST /api/auth/login
    // ----------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/login: returns 200 with tokens on valid credentials")
    void login_returns200WithTokens() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .userId(1L)
                .email("alice@relearn.edu")
                .role("STUDENT")
                .fullName("Alice Johnson")
                .mustChangePassword(false)
                .build();

        when(authService.login(any())).thenReturn(response);

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@relearn.edu");
        req.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/login: returns 401 on bad credentials")
    void login_returns401OnBadCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@relearn.edu");
        req.setPassword("wrong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login: returns 400 when email is blank")
    void login_returns400WhenEmailBlank() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("");
        req.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login: mustChangePassword=true is passed through")
    void login_passesMustChangePasswordFlag() throws Exception {
        AuthResponse response = AuthResponse.builder()
                .accessToken("token").refreshToken("rtoken")
                .userId(2L).email("bob@relearn.edu").role("TEACHER")
                .mustChangePassword(true)
                .build();

        when(authService.login(any())).thenReturn(response);

        LoginRequest req = new LoginRequest();
        req.setEmail("bob@relearn.edu");
        req.setPassword("genpass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));
    }
}
