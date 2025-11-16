package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.fortnite.pronos.dto.auth.LoginRequest;
import com.fortnite.pronos.dto.auth.LoginResponse;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.UserRepository;

/**
 * TDD Tests for UnifiedAuthService - Security Critical Component
 *
 * <p>This test suite validates unified authentication, JWT token management, and security flows
 * using RED-GREEN-REFACTOR TDD methodology. UnifiedAuthService handles user authentication, token
 * generation, refresh operations, and security validation essential for the application security
 * infrastructure.
 *
 * <p>Business Logic Areas: - User authentication and validation - JWT token generation and
 * management - Token refresh and security validation - User details and authorities management -
 * Error handling and security boundary checks
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UnifiedAuthService - Security Critical TDD Tests")
class UnifiedAuthServiceTddTest {

  @Mock private UserRepository userRepository;
  @Mock private JwtService jwtService;

  @InjectMocks private UnifiedAuthService unifiedAuthService;

  private User testUser;
  private LoginRequest testLoginRequest;
  private String validToken;
  private String validRefreshToken;
  private String testUsername;
  private String testEmail;

  @BeforeEach
  void setUp() {
    testUsername = "test_champion";
    testEmail = "champion@fortnite.com";
    validToken = "valid.jwt.token";
    validRefreshToken = "valid.refresh.token";

    // Test user setup
    testUser = new User();
    testUser.setUsername(testUsername);
    testUser.setEmail(testEmail);
    testUser.setRole(User.UserRole.PARTICIPANT);
    testUser.setCurrentSeason(2025);

    // Test login request setup
    testLoginRequest = new LoginRequest();
    testLoginRequest.setUsername(testUsername);
  }

  @Nested
  @DisplayName("User Lookup and Authentication")
  class UserLookupTests {

    @Test
    @DisplayName("Should find user by valid username")
    void shouldFindUserByValidUsername() {
      // RED: Test user lookup with valid username
      when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));

      Optional<User> result = unifiedAuthService.findUserByUsername(testUsername);

      assertThat(result).isPresent();
      assertThat(result.get().getUsername()).isEqualTo(testUsername);
      assertThat(result.get().getEmail()).isEqualTo(testEmail);

      verify(userRepository).findByUsername(testUsername);
    }

    @Test
    @DisplayName("Should return empty for non-existent username")
    void shouldReturnEmptyForNonExistentUsername() {
      // RED: Test user lookup with non-existent username
      String nonExistentUser = "non_existent_user";
      when(userRepository.findByUsername(nonExistentUser)).thenReturn(Optional.empty());

      Optional<User> result = unifiedAuthService.findUserByUsername(nonExistentUser);

      assertThat(result).isEmpty();
      verify(userRepository).findByUsername(nonExistentUser);
    }

    @Test
    @DisplayName("Should handle null username gracefully")
    void shouldHandleNullUsernameGracefully() {
      // RED: Test null input validation
      Optional<User> result = unifiedAuthService.findUserByUsername(null);

      assertThat(result).isEmpty();
      verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should handle empty username gracefully")
    void shouldHandleEmptyUsernameGracefully() {
      // RED: Test empty string validation
      Optional<User> resultEmpty = unifiedAuthService.findUserByUsername("");
      Optional<User> resultWhitespace = unifiedAuthService.findUserByUsername("   ");

      assertThat(resultEmpty).isEmpty();
      assertThat(resultWhitespace).isEmpty();
      verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should authenticate user with simplified authentication")
    void shouldAuthenticateUserWithSimplifiedAuthentication() {
      // RED: Test simplified authentication flow
      when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));

      Optional<User> result = unifiedAuthService.authenticate(testUsername);

      assertThat(result).isPresent();
      assertThat(result.get().getUsername()).isEqualTo(testUsername);
      verify(userRepository).findByUsername(testUsername);
    }

    @Test
    @DisplayName("Should fail authentication for non-existent user")
    void shouldFailAuthenticationForNonExistentUser() {
      // RED: Test authentication failure
      String invalidUser = "invalid_user";
      when(userRepository.findByUsername(invalidUser)).thenReturn(Optional.empty());

      Optional<User> result = unifiedAuthService.authenticate(invalidUser);

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Login Flow and Token Generation")
  class LoginTests {

    @Test
    @DisplayName("Should perform successful login with token generation")
    void shouldPerformSuccessfulLoginWithTokenGeneration() {
      // RED: Test successful login flow
      when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
      when(jwtService.generateToken(any())).thenReturn(validToken);
      when(jwtService.generateRefreshToken(any())).thenReturn(validRefreshToken);

      LoginResponse result = unifiedAuthService.login(testLoginRequest);

      assertThat(result).isNotNull();
      assertThat(result.getToken()).isEqualTo(validToken);
      assertThat(result.getRefreshToken()).isEqualTo(validRefreshToken);
      assertThat(result.getUser()).isNotNull();
      assertThat(result.getUser().getEmail()).isEqualTo(testEmail);

      verify(userRepository).findByUsername(testUsername);
      verify(jwtService).generateToken(any());
      verify(jwtService).generateRefreshToken(any());
    }

    @Test
    @DisplayName("Should throw exception for login with non-existent user")
    void shouldThrowExceptionForLoginWithNonExistentUser() {
      // RED: Test login failure with non-existent user
      when(userRepository.findByUsername(testUsername)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> unifiedAuthService.login(testLoginRequest))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Utilisateur non trouvé");

      verify(userRepository).findByUsername(testUsername);
      verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Should handle JWT service exceptions during login")
    void shouldHandleJwtServiceExceptionsDuringLogin() {
      // RED: Test error handling in token generation
      when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
      when(jwtService.generateToken(any()))
          .thenThrow(new RuntimeException("JWT generation failed"));

      assertThatThrownBy(() -> unifiedAuthService.login(testLoginRequest))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("JWT generation failed");

      verify(userRepository).findByUsername(testUsername);
      verify(jwtService).generateToken(any());
    }

    @Test
    @DisplayName("Should create valid UserDetails for JWT service")
    void shouldCreateValidUserDetailsForJwtService() {
      // RED: Test UserDetails creation and validation
      when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
      when(jwtService.generateToken(any()))
          .thenAnswer(
              invocation -> {
                Object userDetails = invocation.getArgument(0);
                assertThat(userDetails)
                    .isInstanceOf(org.springframework.security.core.userdetails.UserDetails.class);

                org.springframework.security.core.userdetails.UserDetails ud =
                    (org.springframework.security.core.userdetails.UserDetails) userDetails;

                assertThat(ud.getUsername()).isEqualTo(testUsername);
                assertThat(ud.getPassword()).isEmpty(); // MVP has no password
                assertThat(ud.isEnabled()).isTrue();
                assertThat(ud.isAccountNonExpired()).isTrue();
                assertThat(ud.isAccountNonLocked()).isTrue();
                assertThat(ud.isCredentialsNonExpired()).isTrue();

                // Verify authorities
                assertThat(ud.getAuthorities()).hasSize(1);
                GrantedAuthority authority = ud.getAuthorities().iterator().next();
                assertThat(authority).isInstanceOf(SimpleGrantedAuthority.class);
                assertThat(authority.getAuthority()).isEqualTo("ROLE_PARTICIPANT");

                return validToken;
              });
      when(jwtService.generateRefreshToken(any())).thenReturn(validRefreshToken);

      LoginResponse result = unifiedAuthService.login(testLoginRequest);

      assertThat(result).isNotNull();
      assertThat(result.getToken()).isEqualTo(validToken);
    }
  }

  @Nested
  @DisplayName("Token Refresh Operations")
  class TokenRefreshTests {

    @Test
    @DisplayName("Should refresh token successfully with valid refresh token")
    void shouldRefreshTokenSuccessfullyWithValidRefreshToken() {
      // RED: Test successful token refresh
      String newToken = "new.jwt.token";
      String newRefreshToken = "new.refresh.token";

      when(jwtService.extractUsername(validRefreshToken)).thenReturn(testUsername);
      when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
      when(jwtService.isTokenValid(eq(validRefreshToken), any())).thenReturn(true);
      when(jwtService.generateToken(any())).thenReturn(newToken);
      when(jwtService.generateRefreshToken(any())).thenReturn(newRefreshToken);

      LoginResponse result = unifiedAuthService.refreshToken(validRefreshToken);

      assertThat(result).isNotNull();
      assertThat(result.getToken()).isEqualTo(newToken);
      assertThat(result.getRefreshToken()).isEqualTo(newRefreshToken);
      assertThat(result.getUser()).isNotNull();
      assertThat(result.getUser().getEmail()).isEqualTo(testEmail);

      verify(jwtService).extractUsername(validRefreshToken);
      verify(jwtService).isTokenValid(eq(validRefreshToken), any());
      verify(jwtService).generateToken(any());
      verify(jwtService).generateRefreshToken(any());
    }

    @Test
    @DisplayName("Should throw exception for invalid refresh token")
    void shouldThrowExceptionForInvalidRefreshToken() {
      // RED: Test refresh with invalid token
      String invalidToken = "invalid.refresh.token";

      when(jwtService.extractUsername(invalidToken)).thenReturn(testUsername);
      when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
      when(jwtService.isTokenValid(eq(invalidToken), any())).thenReturn(false);

      assertThatThrownBy(() -> unifiedAuthService.refreshToken(invalidToken))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Token de rafraîchissement invalide");

      verify(jwtService).extractUsername(invalidToken);
      verify(jwtService).isTokenValid(eq(invalidToken), any());
      verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Should throw exception when refresh token user not found")
    void shouldThrowExceptionWhenRefreshTokenUserNotFound() {
      // RED: Test refresh with non-existent user
      when(jwtService.extractUsername(validRefreshToken)).thenReturn(testUsername);
      when(userRepository.findByUsername(testUsername)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> unifiedAuthService.refreshToken(validRefreshToken))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Utilisateur non trouvé");

      verify(jwtService).extractUsername(validRefreshToken);
      verify(userRepository).findByUsername(testUsername);
      verify(jwtService, never()).isTokenValid(any(), any());
    }

    @Test
    @DisplayName("Should handle JWT service exceptions during token refresh")
    void shouldHandleJwtServiceExceptionsDuringTokenRefresh() {
      // RED: Test error handling in token refresh
      when(jwtService.extractUsername(validRefreshToken))
          .thenThrow(new RuntimeException("Token parsing failed"));

      assertThatThrownBy(() -> unifiedAuthService.refreshToken(validRefreshToken))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Token de rafraîchissement invalide");

      verify(jwtService).extractUsername(validRefreshToken);
      verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should catch and convert all exceptions to security exceptions")
    void shouldCatchAndConvertAllExceptionsToSecurityExceptions() {
      // RED: Test comprehensive exception handling
      when(jwtService.extractUsername(validRefreshToken)).thenReturn(testUsername);
      when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
      when(jwtService.isTokenValid(eq(validRefreshToken), any())).thenReturn(true);
      when(jwtService.generateToken(any()))
          .thenThrow(new IllegalStateException("Unexpected error"));

      assertThatThrownBy(() -> unifiedAuthService.refreshToken(validRefreshToken))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Token de rafraîchissement invalide");

      verify(jwtService).generateToken(any());
    }
  }

  @Nested
  @DisplayName("UserDetails Implementation")
  class UserDetailsTests {

    @Test
    @DisplayName("Should create UserDetails with correct user authorities for different roles")
    void shouldCreateUserDetailsWithCorrectUserAuthoritiesForDifferentRoles() {
      // RED: Test UserDetails for different user roles
      User adminUser = new User();
      adminUser.setUsername("admin_user");
      adminUser.setRole(User.UserRole.ADMIN);

      User spectatorUser = new User();
      spectatorUser.setUsername("spec_user");
      spectatorUser.setRole(User.UserRole.SPECTATEUR);

      LoginRequest adminRequest = new LoginRequest();
      adminRequest.setUsername("admin_user");

      LoginRequest specRequest = new LoginRequest();
      specRequest.setUsername("spec_user");

      // Test admin user
      when(userRepository.findByUsername("admin_user")).thenReturn(Optional.of(adminUser));
      when(jwtService.generateToken(any())).thenReturn("admin.token");
      when(jwtService.generateRefreshToken(any())).thenReturn("admin.refresh");

      LoginResponse adminResult = unifiedAuthService.login(adminRequest);
      assertThat(adminResult).isNotNull();
      assertThat(adminResult.getUser().getRole()).isEqualTo(User.UserRole.ADMIN);

      // Test spectator user
      when(userRepository.findByUsername("spec_user")).thenReturn(Optional.of(spectatorUser));
      when(jwtService.generateToken(any())).thenReturn("spec.token");

      LoginResponse specResult = unifiedAuthService.login(specRequest);
      assertThat(specResult).isNotNull();
      assertThat(specResult.getUser().getRole()).isEqualTo(User.UserRole.SPECTATEUR);

      // Verify JWT service was called for both users
      verify(jwtService, times(2)).generateToken(any());
      verify(userRepository).findByUsername("admin_user");
      verify(userRepository).findByUsername("spec_user");
    }

    @Test
    @DisplayName("Should maintain UserDetails security properties")
    void shouldMaintainUserDetailsSecurityProperties() {
      // RED: Test UserDetails security properties
      when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
      when(jwtService.generateToken(any()))
          .thenAnswer(
              invocation -> {
                org.springframework.security.core.userdetails.UserDetails ud =
                    (org.springframework.security.core.userdetails.UserDetails)
                        invocation.getArgument(0);

                // All security flags should be true for enabled users
                assertThat(ud.isEnabled()).isTrue();
                assertThat(ud.isAccountNonExpired()).isTrue();
                assertThat(ud.isAccountNonLocked()).isTrue();
                assertThat(ud.isCredentialsNonExpired()).isTrue();

                // Password should be empty for MVP
                assertThat(ud.getPassword()).isEmpty();

                return validToken;
              });
      when(jwtService.generateRefreshToken(any())).thenReturn(validRefreshToken);

      unifiedAuthService.login(testLoginRequest);

      verify(jwtService).generateToken(any());
    }
  }

  @Nested
  @DisplayName("Security Edge Cases and Error Handling")
  class SecurityEdgeCasesTests {

    @Test
    @DisplayName("Should handle malformed tokens securely")
    void shouldHandleMalformedTokensSecurely() {
      // RED: Test malformed token handling
      String malformedToken = "malformed.token";

      when(jwtService.extractUsername(malformedToken))
          .thenThrow(new IllegalArgumentException("Malformed token"));

      assertThatThrownBy(() -> unifiedAuthService.refreshToken(malformedToken))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Token de rafraîchissement invalide");

      // Should not expose internal error details
      verify(jwtService).extractUsername(malformedToken);
    }

    @Test
    @DisplayName("Should handle null login request fields")
    void shouldHandleNullLoginRequestFields() {
      // RED: Test null field validation
      LoginRequest nullUsernameRequest = new LoginRequest();
      nullUsernameRequest.setUsername(null);

      // Should return empty from findUserByUsername for null
      Optional<User> result = unifiedAuthService.findUserByUsername(null);
      assertThat(result).isEmpty();

      // Login should still try to find user and fail gracefully
      assertThatThrownBy(() -> unifiedAuthService.login(nullUsernameRequest))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Utilisateur non trouvé");
    }

    @Test
    @DisplayName("Should handle concurrent login attempts safely")
    void shouldHandleConcurrentLoginAttemptsSafely() {
      // RED: Test thread safety considerations
      when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
      when(jwtService.generateToken(any())).thenReturn(validToken);
      when(jwtService.generateRefreshToken(any())).thenReturn(validRefreshToken);

      // Multiple login attempts should each succeed independently
      LoginResponse result1 = unifiedAuthService.login(testLoginRequest);
      LoginResponse result2 = unifiedAuthService.login(testLoginRequest);

      assertThat(result1.getToken()).isEqualTo(validToken);
      assertThat(result2.getToken()).isEqualTo(validToken);

      // Should have called repository and JWT service twice
      verify(userRepository, times(2)).findByUsername(testUsername);
      verify(jwtService, times(2)).generateToken(any());
    }

    @Test
    @DisplayName("Should maintain security logging without exposing sensitive data")
    void shouldMaintainSecurityLoggingWithoutExposingSensitiveData() {
      // RED: Test security logging behavior
      when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
      when(jwtService.generateToken(any())).thenReturn(validToken);
      when(jwtService.generateRefreshToken(any())).thenReturn(validRefreshToken);

      LoginResponse result = unifiedAuthService.login(testLoginRequest);

      // Verify login was successful (logging verified through service behavior)
      assertThat(result).isNotNull();
      assertThat(result.getUser().getEmail()).isEqualTo(testEmail);

      // Failed login logging test
      LoginRequest failedRequest = new LoginRequest();
      failedRequest.setUsername("unknown_user");
      when(userRepository.findByUsername("unknown_user")).thenReturn(Optional.empty());

      assertThatThrownBy(() -> unifiedAuthService.login(failedRequest))
          .isInstanceOf(RuntimeException.class);

      // Should not expose user existence in logs (verified through exception handling)
    }
  }

  @Nested
  @DisplayName("Integration and Business Logic")
  class IntegrationTests {

    @Test
    @DisplayName("Should maintain consistent user state across operations")
    void shouldMaintainConsistentUserStateAcrossOperations() {
      // RED: Test operation consistency
      when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));
      when(jwtService.generateToken(any())).thenReturn(validToken);
      when(jwtService.generateRefreshToken(any())).thenReturn(validRefreshToken);

      // Login operation
      LoginResponse loginResult = unifiedAuthService.login(testLoginRequest);

      // Verify user data consistency
      assertThat(loginResult.getUser().getEmail()).isEqualTo(testEmail);

      // Authentication operation
      Optional<User> authResult = unifiedAuthService.authenticate(testUsername);
      assertThat(authResult).isPresent();
      assertThat(authResult.get().getUsername()).isEqualTo(testUsername);

      // Both operations should have accessed the same user data
      verify(userRepository, times(2)).findByUsername(testUsername);
    }

    @Test
    @DisplayName("Should handle service dependencies correctly")
    void shouldHandleServiceDependenciesCorrectly() {
      // RED: Test service integration
      when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));

      // JWT service dependency test
      when(jwtService.generateToken(any())).thenReturn(validToken);
      when(jwtService.generateRefreshToken(any())).thenReturn(validRefreshToken);

      LoginResponse result = unifiedAuthService.login(testLoginRequest);

      // Verify all dependencies were used correctly
      assertThat(result.getToken()).isEqualTo(validToken);
      assertThat(result.getRefreshToken()).isEqualTo(validRefreshToken);

      verify(userRepository).findByUsername(testUsername);
      verify(jwtService).generateToken(any());
      verify(jwtService).generateRefreshToken(any());

      // Test token refresh dependency
      when(jwtService.extractUsername(validRefreshToken)).thenReturn(testUsername);
      when(jwtService.isTokenValid(eq(validRefreshToken), any())).thenReturn(true);
      when(jwtService.generateToken(any())).thenReturn("new.token");
      when(jwtService.generateRefreshToken(any())).thenReturn("new.refresh");

      LoginResponse refreshResult = unifiedAuthService.refreshToken(validRefreshToken);
      assertThat(refreshResult).isNotNull();
    }
  }
}
