package com.fortnite.pronos.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * OpenAPI/Swagger Configuration for Fortnite Pronos API
 *
 * <p>This configuration provides comprehensive API documentation with: - Detailed endpoint
 * descriptions - Authentication schemes - Request/response examples - Environment-specific servers
 *
 * <p>Access Swagger UI at: http://localhost:8080/swagger-ui.html Access OpenAPI JSON at:
 * http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

  @Value("${fortnite.pronos.version:1.0.0}")
  private String apiVersion;

  @Value("${server.port:8080}")
  private String serverPort;

  @Bean
  public OpenAPI fortnitePronosOpenAPI() {
    return new OpenAPI()
        .info(createApiInfo())
        .servers(createServers())
        .components(createComponents())
        .security(List.of(createSecurityRequirement()))
        .tags(createApiTags());
  }

  private Info createApiInfo() {
    return new Info()
        .title("Fortnite Pronos API")
        .description(
            """
                    # Fortnite Fantasy League API

                    A comprehensive REST API for managing Fortnite fantasy leagues, including:

                    ## Core Features
                    - **Game Management**: Create, join, and manage fantasy leagues
                    - **Draft System**: Player selection and team building
                    - **Leaderboards**: Real-time rankings and statistics
                    - **Player Performance**: Fortnite player stats and scoring
                    - **Team Management**: Squad composition and player swaps
                    - **Authentication**: Secure JWT-based user management

                    ## API Design Principles
                    - RESTful design with standard HTTP methods
                    - Consistent JSON response formats
                    - Comprehensive error handling with meaningful messages
                    - Pagination support for large data sets
                    - Rate limiting for API protection

                    ## Authentication
                    Most endpoints require JWT authentication. Include the Bearer token in the Authorization header:
                    ```
                    Authorization: Bearer <your-jwt-token>
                    ```

                    ## Response Format
                    All responses follow a consistent format:
                    ```json
                    {
                      "success": true,
                      "data": { ... },
                      "message": "Operation completed successfully",
                      "timestamp": "2025-08-03T10:30:00Z"
                    }
                    ```
                    """)
        .version(apiVersion)
        .contact(
            new Contact()
                .name("Fortnite Pronos Support")
                .email("support@fortnite-pronos.com")
                .url("https://fortnite-pronos.com/support"))
        .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT"));
  }

  private List<Server> createServers() {
    return List.of(
        new Server().url("http://localhost:" + serverPort).description("Development Server"),
        new Server().url("https://api.fortnite-pronos.com").description("Production Server"),
        new Server().url("https://staging-api.fortnite-pronos.com").description("Staging Server"));
  }

  private Components createComponents() {
    return new Components()
        .addSecuritySchemes(
            "bearerAuth",
            new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT token obtained from /api/auth/login endpoint"));
  }

  private SecurityRequirement createSecurityRequirement() {
    return new SecurityRequirement().addList("bearerAuth");
  }

  private List<Tag> createApiTags() {
    return List.of(
        new Tag().name("Authentication").description("User authentication and token management"),
        new Tag().name("Games").description("Fantasy league game management and participation"),
        new Tag().name("Draft").description("Player selection and draft management"),
        new Tag().name("Teams").description("Team composition and player management"),
        new Tag().name("Players").description("Fortnite player statistics and information"),
        new Tag().name("Leaderboards").description("Rankings and performance statistics"),
        new Tag().name("Scores").description("Player scoring and performance tracking"),
        new Tag().name("System").description("System health and diagnostic endpoints"));
  }
}
