package com.relearn.assignment.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 3 configuration for the Relearn Assignment Service.
 * Access at: http://localhost:8083/swagger-ui/index.html
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Relearn Assignment Service API",
        version = "1.0.0",
        description = "Assignment and submission management API. " +
                      "Students can view assignments, submit work, and track submission status. " +
                      "Teachers can create and manage assignments."
    ),
    servers = @Server(url = "http://localhost:8083", description = "Local Development Server")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT token from Auth Service POST /api/auth/login"
)
public class SwaggerConfig {}
