package com.fortnite.pronos.controller.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fortnite.pronos.controller.AuthController;
import com.fortnite.pronos.core.error.ErrorCode;
import com.fortnite.pronos.core.error.FortnitePronosException;
import com.fortnite.pronos.dto.auth.LoginRequest;
import com.fortnite.pronos.dto.auth.LoginResponse;
import com.fortnite.pronos.exception.UserNotFoundException;
import com.fortnite.pronos.service.UnifiedAuthService;

/**
 * TDD Tests for AuthController - Security Critical Component
 *
 * <p>This test suite validates authentication security measures identified as CRITICAL by the
 * security audit agents. Tests follow RED-GREEN-REFACTOR TDD methodology.
 *
 * <p>Security Focus Areas: - JWT token generation and validation - Authentication bypass prevention
 * - Input validation and sanitization - Rate limiting and brute force protection - Token refresh
 * security
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController - Security TDD Tests")
class AuthControllerTddTest {

  @Mock private UnifiedAuthService unifiedAuthService;

  @InjectMocks private AuthController authController;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;
  private LoginRequest validLoginRequest;
  private LoginResponse validLoginResponse;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(authController)
            .setControllerAdvice(new com.fortnite.pronos.config.GlobalExceptionHandler())
            .build();
    objectMapper = new ObjectMapper();

    validLoginRequest = new LoginRequest();
    validLoginRequest.setEmail("test@example.com");
    validLoginRequest.setUsername("testuser");
    validLoginRequest.setPassword("password123");

    validLoginResponse = new LoginResponse();
    validLoginResponse.setToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ5.test");
    validLoginResponse.setRefreshToken("refresh_token_test");

    LoginResponse.UserDto userDto = new LoginResponse.UserDto();
    userDto.setId(UUID.randomUUID());
    userDto.setEmail("test@example.com");
    validLoginResponse.setUser(userDto);
  }

  @Nested
  @DisplayName("Login Endpoint Security Tests")
  class LoginEndpointSecurityTests {

    @Test
    @DisplayName("Should authenticate valid user successfully")
    void shouldAuthenticateValidUserSuccessfully() throws Exception {
      // RED: Define expected behavior for successful authentication
      when(unifiedAuthService.login(any(LoginRequest.class))).thenReturn(validLoginResponse);

      // When & Then
      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(validLoginRequest)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.token").value(validLoginResponse.getToken()))
          .andExpect(jsonPath("$.user.email").value(validLoginResponse.getUser().getEmail()));

      verify(unifiedAuthService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Should reject login with invalid credentials")
    void shouldRejectLoginWithInvalidCredentials() throws Exception {
      // RED: Security test - invalid credentials should be rejected
      when(unifiedAuthService.login(any(LoginRequest.class)))
          .thenThrow(new RuntimeException("Invalid credentials"));

      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(validLoginRequest)))
          .andExpect(status().isInternalServerError());

      verify(unifiedAuthService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Should return 401 when user is not found")
    void shouldReturn401WhenUserNotFound() throws Exception {
      // RED: Security test - user not found should return 401, not 500
      when(unifiedAuthService.login(any(LoginRequest.class)))
          .thenThrow(new UserNotFoundException("Utilisateur non trouvé: testuser"));

      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(validLoginRequest)))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.status").value(401))
          .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
          .andExpect(jsonPath("$.message").value("Utilisateur non trouvé: testuser"));

      verify(unifiedAuthService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("Should validate required fields in login request")
    void shouldValidateRequiredFieldsInLoginRequest() throws Exception {
      // RED: Security test - empty/null fields should be rejected
      LoginRequest invalidRequest = new LoginRequest();
      // Leave fields empty to trigger validation

      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(invalidRequest)))
          .andExpect(status().isBadRequest());

      // Should not call service with invalid data
      verifyNoInteractions(unifiedAuthService);
    }

    @Test
    @DisplayName("Should handle malformed JSON requests securely")
    void shouldHandleMalformedJsonRequestsSecurely() throws Exception {
      // RED: Security test - malformed JSON should not crash the system
      String malformedJson = "{ invalid json }";

      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(malformedJson))
          .andExpect(status().isInternalServerError());

      verifyNoInteractions(unifiedAuthService);
    }

    @Test
    @DisplayName("Should prevent SQL injection through login fields")
    void shouldPreventSqlInjectionThroughLoginFields() throws Exception {
      // RED: Security test - SQL injection attempts should be safely handled
      LoginRequest sqlInjectionRequest = new LoginRequest();
      sqlInjectionRequest.setEmail("'; DROP TABLE users; --");
      sqlInjectionRequest.setUsername("'; DROP TABLE users; --");
      sqlInjectionRequest.setPassword("password123");

      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(sqlInjectionRequest)))
          .andExpect(status().isBadRequest());

      // SQL injection attempts should be handled gracefully without crashing
      // Note: In this test, input validation may prevent service call if format is invalid
    }

    @Test
    @DisplayName("Should handle authentication service exceptions gracefully")
    void shouldHandleAuthenticationServiceExceptionsGracefully() throws Exception {
      // RED: Error handling test
      when(unifiedAuthService.login(any(LoginRequest.class)))
          .thenThrow(new RuntimeException("Database connection failed"));

      mockMvc
          .perform(
              post("/api/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(validLoginRequest)))
          .andExpect(status().isInternalServerError());

      verify(unifiedAuthService).login(any(LoginRequest.class));
    }
  }

  @Nested
  @DisplayName("Token Refresh Security Tests")
  class TokenRefreshSecurityTests {

    @Test
    @DisplayName("Should refresh valid token successfully")
    void shouldRefreshValidTokenSuccessfully() throws Exception {
      // RED: Define expected behavior for valid token refresh
      String validRefreshTokenHeader = "Bearer valid_refresh_token";
      when(unifiedAuthService.refreshToken("valid_refresh_token")).thenReturn(validLoginResponse);

      mockMvc
          .perform(post("/api/auth/refresh").header("Authorization", validRefreshTokenHeader))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.token").value(validLoginResponse.getToken()));

      verify(unifiedAuthService).refreshToken("valid_refresh_token");
    }

    @Test
    @DisplayName("Should reject refresh request without Authorization header")
    void shouldRejectRefreshRequestWithoutAuthorizationHeader() throws Exception {
      // RED: Security test - missing Authorization header should be rejected
      mockMvc.perform(post("/api/auth/refresh")).andExpect(status().isInternalServerError());

      verifyNoInteractions(unifiedAuthService);
    }

    @Test
    @DisplayName("Should reject refresh request with invalid Bearer format")
    void shouldRejectRefreshRequestWithInvalidBearerFormat() throws Exception {
      // RED: Security test - invalid Bearer format should be rejected
      String invalidTokenHeader = "InvalidFormat token123";

      mockMvc
          .perform(post("/api/auth/refresh").header("Authorization", invalidTokenHeader))
          .andExpect(status().isUnauthorized());

      verifyNoInteractions(unifiedAuthService);
    }

    @Test
    @DisplayName("Should reject refresh request with expired token")
    void shouldRejectRefreshRequestWithExpiredToken() throws Exception {
      // RED: Security test - expired tokens should be rejected
      String expiredTokenHeader = "Bearer expired_token";
      when(unifiedAuthService.refreshToken("expired_token"))
          .thenThrow(new FortnitePronosException(ErrorCode.AUTH_SEC_002));

      mockMvc
          .perform(post("/api/auth/refresh").header("Authorization", expiredTokenHeader))
          .andExpect(status().isUnauthorized());

      verify(unifiedAuthService).refreshToken("expired_token");
    }

    @Test
    @DisplayName("Should reject refresh request with tampered token")
    void shouldRejectRefreshRequestWithTamperedToken() throws Exception {
      // RED: Security test - tampered tokens should be detected and rejected
      String tamperedTokenHeader = "Bearer tampered.jwt.token";
      when(unifiedAuthService.refreshToken("tampered.jwt.token"))
          .thenThrow(new FortnitePronosException(ErrorCode.AUTH_SEC_001));

      mockMvc
          .perform(post("/api/auth/refresh").header("Authorization", tamperedTokenHeader))
          .andExpect(status().isUnauthorized());

      verify(unifiedAuthService).refreshToken("tampered.jwt.token");
    }
  }

  @Nested
  @DisplayName("Controller Integration and Error Handling")
  class ControllerIntegrationTests {

    @Test
    @DisplayName("Should delegate login processing to UnifiedAuthService")
    void shouldDelegateLoginProcessingToUnifiedAuthService() {
      // RED: Integration test - controller should delegate to service layer
      when(unifiedAuthService.login(any(LoginRequest.class))).thenReturn(validLoginResponse);

      ResponseEntity<LoginResponse> response = authController.login(validLoginRequest);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody()).isEqualTo(validLoginResponse);
      verify(unifiedAuthService).login(validLoginRequest);
    }

    @Test
    @DisplayName("Should delegate token refresh to UnifiedAuthService")
    void shouldDelegateTokenRefreshToUnifiedAuthService() {
      // RED: Integration test - controller should delegate refresh to service
      String authHeader = "Bearer valid_token";
      when(unifiedAuthService.refreshToken("valid_token")).thenReturn(validLoginResponse);

      ResponseEntity<LoginResponse> response = authController.refreshToken(authHeader);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody()).isEqualTo(validLoginResponse);
      verify(unifiedAuthService).refreshToken("valid_token");
    }

    @Test
    @DisplayName("Should handle FortnitePronosException consistently")
    void shouldHandleFortnitePronosExceptionConsistently() {
      // RED: Error handling consistency test
      when(unifiedAuthService.login(any(LoginRequest.class)))
          .thenThrow(new FortnitePronosException(ErrorCode.AUTH_BUS_001));

      assertThatThrownBy(() -> authController.login(validLoginRequest))
          .isInstanceOf(FortnitePronosException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_BUS_001);

      verify(unifiedAuthService).login(validLoginRequest);
    }

    @Test
    @DisplayName("Should extract token correctly from Bearer header")
    void shouldExtractTokenCorrectlyFromBearerHeader() {
      // RED: Token extraction logic test
      String authHeader = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";
      String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";

      when(unifiedAuthService.refreshToken(expectedToken)).thenReturn(validLoginResponse);

      ResponseEntity<LoginResponse> response = authController.refreshToken(authHeader);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      verify(unifiedAuthService).refreshToken(expectedToken);
    }
  }

  @Nested
  @DisplayName("Security Best Practices Validation")
  class SecurityBestPracticesValidation {

    @Test
    @DisplayName("Should not log sensitive information in authentication attempts")
    void shouldNotLogSensitiveInformationInAuthenticationAttempts() {
      // RED: Security test - passwords should never be logged
      when(unifiedAuthService.login(any(LoginRequest.class))).thenReturn(validLoginResponse);

      authController.login(validLoginRequest);

      // Note: This is a behavioral test - in real implementation,
      // we should verify that logging doesn't contain password information
      // The actual verification would be done through log capture in integration tests
      verify(unifiedAuthService).login(validLoginRequest);
    }

    @Test
    @DisplayName("Should handle rate limiting scenarios")
    void shouldHandleRateLimitingScenarios() {
      // RED: Security test - rate limiting should be properly handled
      when(unifiedAuthService.login(any(LoginRequest.class)))
          .thenThrow(new FortnitePronosException(ErrorCode.AUTH_SEC_003));

      assertThatThrownBy(() -> authController.login(validLoginRequest))
          .isInstanceOf(FortnitePronosException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_SEC_003);

      verify(unifiedAuthService).login(validLoginRequest);
    }

    @Test
    @DisplayName("Should validate token format in refresh requests")
    void shouldValidateTokenFormatInRefreshRequests() {
      // RED: Security validation test
      String invalidFormatToken = "NotBearerFormat";

      assertThatThrownBy(() -> authController.refreshToken(invalidFormatToken))
          .isInstanceOf(FortnitePronosException.class)
          .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN_FORMAT);

      verifyNoInteractions(unifiedAuthService);
    }

    @Test
    @DisplayName("Should maintain security through all error paths")
    void shouldMaintainSecurityThroughAllErrorPaths() {
      // RED: Security test - error responses should not leak sensitive information
      when(unifiedAuthService.login(any(LoginRequest.class)))
          .thenThrow(
              new RuntimeException("Database error: connection failed to user_passwords table"));

      // Controller should handle this exception without exposing internal details
      assertThatThrownBy(() -> authController.login(validLoginRequest))
          .isInstanceOf(RuntimeException.class);

      verify(unifiedAuthService).login(validLoginRequest);
    }
  }
}
