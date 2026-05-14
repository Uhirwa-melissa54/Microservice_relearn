package com.relearn.auth.security;

import com.relearn.auth.jwt.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter — runs once per request.
 *
 * Flow:
 *  1. Extract the JWT from the "Authorization: Bearer <token>" header
 *  2. Validate the token
 *  3. Load the user from the database
 *  4. Set the authentication in the SecurityContext
 *
 * If any step fails, the request continues without authentication
 * and Spring Security will reject it at the endpoint level.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Step 1: Read the Authorization header
        final String authHeader = request.getHeader("Authorization");

        // If there's no Bearer token, skip this filter entirely
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 2: Extract the token (remove "Bearer " prefix)
        final String jwt = authHeader.substring(7);

        try {
            // Step 3: Validate token structure and signature
            if (!jwtUtils.validateToken(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Step 4: Extract the email from the token
            final String email = jwtUtils.extractEmail(jwt);

            // Only proceed if we have an email and no authentication is set yet
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Step 5: Load user from database to verify they still exist
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Step 6: Double-check token is valid against the loaded user
                if (jwtUtils.isTokenValid(jwt, userDetails)) {

                    // Step 7: Create authentication token and set it in the SecurityContext
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,                          // credentials not needed after auth
                                    userDetails.getAuthorities()   // roles/permissions
                            );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // This tells Spring Security the request is authenticated
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Log the error but don't block the filter chain
            // The request will simply be treated as unauthenticated
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
