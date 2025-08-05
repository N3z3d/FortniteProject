package com.fortnite.pronos.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fortnite.pronos.core.error.ErrorCode;
import com.fortnite.pronos.core.error.FortnitePronosException;
import com.fortnite.pronos.dto.auth.LoginRequest;
import com.fortnite.pronos.dto.auth.LoginResponse;
import com.fortnite.pronos.service.UnifiedAuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Authentication Controller
 *
 * <p>Provides secure user authentication and token management for the Fortnite Pronos API. Supports
 * JWT-based authentication with token refresh capabilities.
 */
@Tag(name = "Authentication", description = "User authentication and token management")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  private final UnifiedAuthService unifiedAuthService;

  /** User login endpoint */
  @Operation(
      summary = "Authenticate user and obtain JWT token",
      description =
          """
        Authenticates a user with email and password, returning a JWT token for subsequent API requests.

        **Security Features:**
        - Password validation with secure hashing
        - JWT token generation with configurable expiration
        - Failed login attempt tracking and rate limiting
        - Secure token refresh capability

        **Token Usage:**
        Include the returned token in subsequent requests:
        ```
        Authorization: Bearer <token>
        ```

        **Development Mode:**
        In development environment, simplified authentication may be available.
        """)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Authentication successful",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponse.class),
                    examples =
                        @ExampleObject(
                            name = "Successful login",
                            value =
                                """
                    {
                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                      "expiresIn": 3600,
                      "user": {
                        "id": "550e8400-e29b-41d4-a716-446655440000",
                        "email": "john.doe@example.com",
                        "username": "john.doe",
                        "role": "USER"
                      }
                    }
                    """))),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            name = "Invalid credentials",
                            value =
                                """
                    {
                      "success": false,
                      "message": "Invalid email or password",
                      "error": {
                        "code": "INVALID_CREDENTIALS",
                        "message": "The provided email or password is incorrect"
                      },
                      "timestamp": "2025-08-03T10:30:00Z"
                    }
                    """))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request format",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            name = "Validation error",
                            value =
                                """
                    {
                      "success": false,
                      "message": "Validation failed",
                      "error": {
                        "code": "VALIDATION_ERROR",
                        "fieldErrors": [
                          {
                            "field": "email",
                            "message": "must be a valid email address",
                            "rejectedValue": "invalid-email"
                          }
                        ]
                      },
                      "timestamp": "2025-08-03T10:30:00Z"
                    }
                    """))),
        @ApiResponse(
            responseCode = "429",
            description = "Too many login attempts",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            name = "Rate limit exceeded",
                            value =
                                """
                    {
                      "success": false,
                      "message": "Too many login attempts",
                      "error": {
                        "code": "RATE_LIMIT_EXCEEDED",
                        "message": "Please wait before attempting to login again",
                        "retryAfter": 300
                      },
                      "timestamp": "2025-08-03T10:30:00Z"
                    }
                    """)))
      })
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(
      @Parameter(
              description = "User login credentials",
              required = true,
              schema = @Schema(implementation = LoginRequest.class))
          @Valid
          @RequestBody
          LoginRequest request) {
    log.debug("Login attempt for user: {}", request.getEmail());

    try {
      return ResponseEntity.ok(unifiedAuthService.login(request));
    } catch (Exception e) {
      log.error("Login failed for user {}: {}", request.getEmail(), e.getMessage());
      throw e;
    }
  }

  /** Endpoint de rafraîchissement de token Clean Code: Validation et sécurisation du token */
  @PostMapping("/refresh")
  public ResponseEntity<LoginResponse> refreshToken(
      @RequestHeader("Authorization") String refreshToken) {
    log.debug("Token refresh requested");

    try {
      // Security: Validation du format du header
      if (refreshToken == null || !refreshToken.startsWith("Bearer ")) {
        throw new FortnitePronosException(ErrorCode.INVALID_TOKEN_FORMAT);
      }

      String token = refreshToken.substring(7);

      return ResponseEntity.ok(unifiedAuthService.refreshToken(token));
    } catch (Exception e) {
      log.error("Token refresh failed: {}", e.getMessage());
      throw e;
    }
  }
}
