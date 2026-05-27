package com.relearn.auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 3 configuration for the Relearn Auth Service.
 *
 * Access Swagger UI at: http://localhost:8081/swagger-ui/index.html
 * Access raw OpenAPI JSON at: http://localhost:8081/v3/api-docs
 *
 * The bearerAuth security scheme enables the "Authorize" button in Swagger UI
 * so you can paste a JWT token and test protected endpoints directly.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Relearn Auth Service API",
        version = "1.0.0",
        description = "Authentication, user management, and admin portal API for the Relearn school platform. " +
                      "Handles login, registration, JWT tokens, user profiles, and admin operations.",
        contact = @Contact(name = "Relearn Platform", email = "dev@relearn.com")
    ),
    servers = {
        @Server(url = "http://localhost:8081", description = "Local Development Server")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Enter your JWT access token. Obtain it from POST /api/auth/login"
)
public class SwaggerConfig {
    // Configuration is done via annotations above.
    // SpringDoc auto-scans all @RestController classes and generates docs.
}
