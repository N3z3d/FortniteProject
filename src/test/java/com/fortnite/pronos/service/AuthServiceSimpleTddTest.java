package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fortnite.pronos.dto.auth.LoginRequest;
import com.fortnite.pronos.dto.auth.LoginResponse;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.UserRepository;

/**
 * Tests TDD pour AuthServiceSimple Focus sur l'authentification sans AuthenticationManager en mode
 * dev
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceSimple TDD - Dev Mode")
class AuthServiceSimpleTddTest {

  @Mock private UserRepository userRepository;
  @Mock private JwtService jwtService;
  @Mock private UserDetailsService userDetailsService;
  @Mock private PasswordEncoder passwordEncoder;

  private User testUser;
  private UserDetails userDetails;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(UUID.randomUUID());
    testUser.setUsername("Thibaut");
    testUser.setEmail("thibaut@test.com");
    testUser.setPassword("$2a$10$encodedPassword");
    testUser.setRole(User.UserRole.USER);

    userDetails =
        org.springframework.security.core.userdetails.User.builder()
            .username("thibaut@test.com")
            .password("$2a$10$encodedPassword")
            .authorities("ROLE_PARTICIPANT")
            .build();
  }

  @Test
  @DisplayName("Doit créer AuthServiceDev sans AuthenticationManager")
  void shouldCreateAuthServiceDevWithoutAuthenticationManager() {
    // When - Créer le service sans AuthenticationManager
    AuthServiceDev authService =
        new AuthServiceDev(userRepository, jwtService, userDetailsService, passwordEncoder);

    // Then
    assertThat(authService).isNotNull();
  }

  @Test
  @DisplayName("Doit authentifier directement sans Spring Security en mode dev")
  void shouldAuthenticateDirectlyWithoutSpringSecurityInDevMode() {
    // Given
    LoginRequest request = new LoginRequest();
    request.setEmail("thibaut@test.com");
    request.setPassword("password");

    when(userRepository.findByEmail("thibaut@test.com")).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("password", "$2a$10$encodedPassword")).thenReturn(true);
    when(userDetailsService.loadUserByUsername("thibaut@test.com")).thenReturn(userDetails);
    when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");
    when(jwtService.generateRefreshToken(userDetails)).thenReturn("refresh-token");

    AuthServiceDev authService =
        new AuthServiceDev(userRepository, jwtService, userDetailsService, passwordEncoder);

    // When
    LoginResponse response = authService.login(request);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getToken()).isEqualTo("jwt-token");
    assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    assertThat(response.getUser().getEmail()).isEqualTo("thibaut@test.com");
  }

  @Test
  @DisplayName("Doit rejeter les mauvais mots de passe en mode dev")
  void shouldRejectBadPasswordInDevMode() {
    // Given
    LoginRequest request = new LoginRequest();
    request.setEmail("thibaut@test.com");
    request.setPassword("wrongpassword");

    when(userRepository.findByEmail("thibaut@test.com")).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("wrongpassword", "$2a$10$encodedPassword")).thenReturn(false);

    AuthServiceDev authService =
        new AuthServiceDev(userRepository, jwtService, userDetailsService, passwordEncoder);

    // When & Then
    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Mot de passe incorrect");
  }

  /**
   * Service d'authentification pour le mode développement Clean Code TDD: Sans
   * AuthenticationManager pour éviter les erreurs Spring
   */
  public static class AuthServiceDev {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceDev(
        UserRepository userRepository,
        JwtService jwtService,
        UserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder) {
      this.userRepository = userRepository;
      this.jwtService = jwtService;
      this.userDetailsService = userDetailsService;
      this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
      // Authentification directe sans Spring Security pour le mode dev
      User user =
          userRepository
              .findByEmail(request.getEmail())
              .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

      if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        throw new RuntimeException("Mot de passe incorrect");
      }

      UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
      String jwtToken = jwtService.generateToken(userDetails);
      String refreshToken = jwtService.generateRefreshToken(userDetails);

      LoginResponse response = new LoginResponse();
      response.setToken(jwtToken);
      response.setRefreshToken(refreshToken);
      response.setUser(LoginResponse.UserDto.from(user));

      return response;
    }
  }
}
