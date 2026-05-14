package com.relearn.notes.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 3 configuration for the Relearn Notes Service.
 * Access at: http://localhost:8082/swagger-ui/index.html
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Relearn Notes Service API",
        version = "1.0.0",
        description = "Notes management API. Students can view, filter, and download notes. " +
                      "Teachers can upload and manage notes."
    ),
    servers = @Server(url = "http://localhost:8082", description = "Local Development Server")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT token from Auth Service POST /api/auth/login"
)
public class SwaggerConfig {}
