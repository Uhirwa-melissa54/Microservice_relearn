package com.discover.APIGateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Global CORS configuration for the API Gateway.
 *
 * This allows the React frontend (running on localhost:5173 via Vite)
 * to communicate with the backend through the gateway without CORS errors.
 *
 * Key points:
 * - Authorization header is explicitly allowed (required for JWT)
 * - OPTIONS preflight requests are handled automatically
 * - Credentials (cookies) are allowed
 * - CORS is configured ONLY here — not in individual services
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow the Vite dev server and any localhost port
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:3000"
        ));

        // Allow all standard HTTP methods
        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Allow all headers — critically includes Authorization for JWT
        config.setAllowedHeaders(List.of("*"));

        // Expose headers the frontend might need to read
        config.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition",
                "Content-Type"
        ));

        // Allow credentials (needed if using cookies alongside JWT)
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour (reduces OPTIONS requests)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply to ALL routes
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
