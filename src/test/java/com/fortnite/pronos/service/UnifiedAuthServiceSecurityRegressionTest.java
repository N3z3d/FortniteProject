package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fortnite.pronos.dto.auth.LoginRequest;
import com.fortnite.pronos.dto.auth.LoginResponse;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnifiedAuthService - Security Regression")
class UnifiedAuthServiceSecurityRegressionTest {

  @Mock private UserRepository userRepository;
  @Mock private JwtService jwtService;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private UnifiedAuthService unifiedAuthService;

  private User user;
  private LoginRequest request;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setUsername("secure_user");
    user.setEmail("secure@fortnite.com");
    user.setPassword("$2a$10$encoded.password.hash.for.tests");
    user.setRole(User.UserRole.USER);

    request = new LoginRequest();
    request.setUsername("secure_user");
    request.setPassword("plain-password");
  }

  @Test
  @DisplayName("Should reject invalid password and never issue JWT")
  void shouldRejectInvalidPasswordAndNeverIssueJwt() {
    when(userRepository.findByUsername("secure_user")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("plain-password", user.getPassword())).thenReturn(false);

    assertThatThrownBy(() -> unifiedAuthService.login(request))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("Identifiants invalides");

    verify(passwordEncoder).matches("plain-password", user.getPassword());
    verify(jwtService, never()).generateToken(any());
    verify(jwtService, never()).generateRefreshToken(any());
  }

  @Test
  @DisplayName("Should issue JWT when password matches")
  void shouldIssueJwtWhenPasswordMatches() {
    when(userRepository.findByUsername("secure_user")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("plain-password", user.getPassword())).thenReturn(true);
    when(jwtService.generateToken(any())).thenReturn("jwt-token");
    when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

    LoginResponse response = unifiedAuthService.login(request);

    assertThat(response.getToken()).isEqualTo("jwt-token");
    assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    verify(jwtService).generateToken(any());
    verify(jwtService).generateRefreshToken(any());
  }

  @Test
  @DisplayName("Should fallback to email when username is missing")
  void shouldFallbackToEmailWhenUsernameIsMissing() {
    LoginRequest emailRequest = new LoginRequest();
    emailRequest.setEmail("secure@fortnite.com");
    emailRequest.setPassword("plain-password");

    when(userRepository.findByEmail("secure@fortnite.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("plain-password", user.getPassword())).thenReturn(true);
    when(jwtService.generateToken(any())).thenReturn("jwt-token");
    when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

    LoginResponse response = unifiedAuthService.login(emailRequest);

    assertThat(response.getToken()).isEqualTo("jwt-token");
    verify(userRepository).findByEmail("secure@fortnite.com");
  }
}
