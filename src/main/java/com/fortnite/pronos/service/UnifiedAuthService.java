package com.fortnite.pronos.service;

import java.util.Optional;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.dto.auth.LoginRequest;
import com.fortnite.pronos.dto.auth.LoginResponse;
import com.fortnite.pronos.exception.UserNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service d'authentification unifie pour le MVP. */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"java:S112"})
public class UnifiedAuthService {

  private static final String INVALID_CREDENTIALS_MESSAGE = "Identifiants invalides";
  private static final String INVALID_REFRESH_TOKEN_MESSAGE = "Token de rafraichissement invalide";

  private final com.fortnite.pronos.repository.UserRepository userRepository;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;

  /** Recupere un utilisateur par nom d'utilisateur. */
  public Optional<com.fortnite.pronos.model.User> findUserByUsername(String username) {
    if (username == null || username.trim().isEmpty()) {
      return Optional.empty();
    }
    return userRepository.findByUsername(username);
  }

  /** Authentifie un utilisateur (version simplifiee pour MVP). */
  public Optional<com.fortnite.pronos.model.User> authenticate(String username) {
    return findUserByUsername(username);
  }

  /** Connexion utilisateur avec generation de token JWT. */
  public LoginResponse login(LoginRequest request) {
    String loginIdentifier = resolveLoginIdentifier(request);
    log.info("Tentative de connexion pour l'utilisateur: {}", loginIdentifier);

    Optional<com.fortnite.pronos.model.User> userOpt = findUserForLogin(loginIdentifier);
    if (userOpt.isEmpty()) {
      log.warn("Utilisateur non trouve: {}", loginIdentifier);
      throw new UserNotFoundException("Utilisateur non trouve: " + loginIdentifier);
    }

    com.fortnite.pronos.model.User user = userOpt.get();
    if (!isPasswordValid(request, user)) {
      log.warn("Echec d'authentification: mot de passe invalide pour {}", loginIdentifier);
      throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
    }

    CustomUserDetails userDetails = new CustomUserDetails(user);

    String token = jwtService.generateToken(userDetails);
    String refreshToken = jwtService.generateRefreshToken(userDetails);

    LoginResponse response = new LoginResponse();
    response.setToken(token);
    response.setRefreshToken(refreshToken);
    response.setUser(LoginResponse.UserDto.from(user));

    log.info("Connexion reussie pour l'utilisateur: {}", user.getEmail());
    return response;
  }

  private String resolveLoginIdentifier(LoginRequest request) {
    if (request == null) {
      return null;
    }
    if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
      return request.getUsername().trim();
    }
    if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
      return request.getEmail().trim();
    }
    return null;
  }

  private Optional<com.fortnite.pronos.model.User> findUserForLogin(String loginIdentifier) {
    if (loginIdentifier == null || loginIdentifier.trim().isEmpty()) {
      return Optional.empty();
    }

    Optional<com.fortnite.pronos.model.User> userByUsername = findUserByUsername(loginIdentifier);
    if (userByUsername.isPresent()) {
      return userByUsername;
    }
    return userRepository.findByEmail(loginIdentifier);
  }

  private boolean isPasswordValid(LoginRequest request, com.fortnite.pronos.model.User user) {
    if (request == null
        || request.getPassword() == null
        || request.getPassword().trim().isEmpty()) {
      return false;
    }

    String storedPasswordHash = user.getPasswordHash();
    if (storedPasswordHash == null || storedPasswordHash.trim().isEmpty()) {
      return false;
    }

    try {
      return passwordEncoder.matches(request.getPassword(), storedPasswordHash);
    } catch (RuntimeException exception) {
      log.warn(
          "Impossible de valider le mot de passe pour {}: {}",
          user.getUsername(),
          exception.getMessage());
      return false;
    }
  }

  /** Actualise le token avec le refresh token. */
  public LoginResponse refreshToken(String refreshToken) {
    log.info("Tentative de rafraichissement de token");

    try {
      String username = jwtService.extractUsername(refreshToken);
      Optional<com.fortnite.pronos.model.User> userOpt = userRepository.findByUsername(username);

      if (userOpt.isEmpty()) {
        log.warn("Utilisateur non trouve pour le token: {}", username);
        throw new UserNotFoundException("Utilisateur non trouve: " + username);
      }

      com.fortnite.pronos.model.User user = userOpt.get();
      CustomUserDetails userDetails = new CustomUserDetails(user);

      if (!jwtService.isTokenValid(refreshToken, userDetails)) {
        log.warn(INVALID_REFRESH_TOKEN_MESSAGE);
        throw new RuntimeException(INVALID_REFRESH_TOKEN_MESSAGE);
      }

      String newToken = jwtService.generateToken(userDetails);
      String newRefreshToken = jwtService.generateRefreshToken(userDetails);

      LoginResponse response = new LoginResponse();
      response.setToken(newToken);
      response.setRefreshToken(newRefreshToken);
      response.setUser(LoginResponse.UserDto.from(user));

      log.info("Token rafraichi avec succes pour l'utilisateur: {}", user.getEmail());
      return response;
    } catch (RuntimeException exception) {
      if (exception instanceof UserNotFoundException) {
        throw exception;
      }
      log.warn("Erreur lors du rafraichissement du token: {}", exception.getMessage());
      throw new RuntimeException(INVALID_REFRESH_TOKEN_MESSAGE);
    } catch (Exception exception) {
      log.warn("Erreur lors du rafraichissement du token: {}", exception.getMessage());
      throw new RuntimeException(INVALID_REFRESH_TOKEN_MESSAGE);
    }
  }

  /** Classe interne pour implementer UserDetails. */
  private static class CustomUserDetails
      implements org.springframework.security.core.userdetails.UserDetails {

    private final String username;
    private final String roleName;

    CustomUserDetails(com.fortnite.pronos.model.User user) {
      this.username = user.getUsername();
      this.roleName = "ROLE_" + user.getRole().name();
    }

    @Override
    public String getUsername() {
      return username;
    }

    @Override
    public String getPassword() {
      return "";
    }

    @Override
    public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority>
        getAuthorities() {
      return java.util.List.of(
          new org.springframework.security.core.authority.SimpleGrantedAuthority(roleName));
    }

    @Override
    public boolean isAccountNonExpired() {
      return true;
    }

    @Override
    public boolean isAccountNonLocked() {
      return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
      return true;
    }

    @Override
    public boolean isEnabled() {
      return true;
    }
  }
}
