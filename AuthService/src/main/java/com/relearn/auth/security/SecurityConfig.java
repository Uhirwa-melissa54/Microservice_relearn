package com.relearn.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Main Spring Security configuration.
 *
 * Key decisions:
 *  - Stateless sessions (no server-side session storage — JWT handles state)
 *  - CSRF disabled (not needed for stateless REST APIs)
 *  - Role-based access control on endpoints
 *  - Custom JWT filter runs before Spring's default auth filter
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // enables @PreAuthorize on controller methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Defines which endpoints are public and which require authentication/roles.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — not needed for stateless JWT-based APIs
            .csrf(AbstractHttpConfigurer::disable)

            // Define authorization rules
            .authorizeHttpRequests(auth -> auth

                // Public endpoints — no token required
                .requestMatchers("/api/auth/**").permitAll()

                // Swagger UI — public access
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/v3/api-docs"
                ).permitAll()

                // User management — ADMIN only
                .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/users/**").hasRole("ADMIN")

                // Any authenticated user can get their own profile
                .requestMatchers(HttpMethod.GET, "/api/users/**").authenticated()

                // Student profile endpoints — any authenticated user
                .requestMatchers("/api/student/**").authenticated()

                // Teacher profile endpoints — TEACHER or ADMIN
                .requestMatchers("/api/teacher/**").hasAnyRole("TEACHER", "ADMIN")

                // Admin portal — ADMIN only
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // All other requests must be authenticated
                .anyRequest().authenticated()
            )

            // Stateless — Spring Security will NOT create HTTP sessions
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Register our custom authentication provider (uses DB + BCrypt)
            .authenticationProvider(authenticationProvider())

            // Add JWT filter BEFORE Spring's default username/password filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configures how Spring Security authenticates users:
     *  - Loads users from the database via UserDetailsService
     *  - Verifies passwords using BCrypt
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * BCrypt password encoder — used to hash passwords on registration
     * and verify them on login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes the AuthenticationManager bean so it can be injected
     * into the AuthService for programmatic authentication during login.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
