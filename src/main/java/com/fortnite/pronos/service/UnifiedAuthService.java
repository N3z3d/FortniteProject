package com.fortnite.pronos.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fortnite.pronos.dto.auth.LoginRequest;
import com.fortnite.pronos.dto.auth.LoginResponse;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service d'authentification unifié pour le MVP */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnifiedAuthService {

  private final UserRepository userRepository;
  private final JwtService jwtService;

  /** Récupère un utilisateur par nom d'utilisateur */
  public Optional<User> findUserByUsername(String username) {
    if (username == null || username.trim().isEmpty()) {
      return Optional.empty();
    }
    return userRepository.findByUsername(username);
  }

  /** Authentifie un utilisateur (version simplifiée pour MVP) */
  public Optional<User> authenticate(String username) {
    return findUserByUsername(username);
  }

  /** Connexion utilisateur avec génération de token JWT */
  public LoginResponse login(LoginRequest request) {
    log.info("Tentative de connexion pour l'utilisateur: {}", request.getUsername());

    // Authentification simplifiée pour MVP
    Optional<User> userOpt = findUserByUsername(request.getUsername());

    if (userOpt.isEmpty()) {
      log.warn("Utilisateur non trouvé: {}", request.getUsername());
      throw new RuntimeException("Utilisateur non trouvé");
    }

    User user = userOpt.get();

    // Création d'un UserDetails pour JwtService
    CustomUserDetails userDetails = new CustomUserDetails(user);

    // Génération des tokens
    String token = jwtService.generateToken(userDetails);
    String refreshToken = jwtService.generateRefreshToken(userDetails);

    LoginResponse response = new LoginResponse();
    response.setToken(token);
    response.setRefreshToken(refreshToken);
    response.setUser(LoginResponse.UserDto.from(user));

    log.info("Connexion réussie pour l'utilisateur: {}", user.getEmail());
    return response;
  }

  /** Actualise le token avec le refresh token */
  public LoginResponse refreshToken(String refreshToken) {
    log.info("Tentative de rafraîchissement de token");

    // Pour le MVP, on simplifie la validation du refresh token
    try {
      String username = jwtService.extractUsername(refreshToken);
      Optional<User> userOpt = userRepository.findByUsername(username);

      if (userOpt.isEmpty()) {
        log.warn("Utilisateur non trouvé pour le token: {}", username);
        throw new RuntimeException("Utilisateur non trouvé");
      }

      User user = userOpt.get();
      CustomUserDetails userDetails = new CustomUserDetails(user);

      // Vérifier si le token est encore valide
      if (!jwtService.isTokenValid(refreshToken, userDetails)) {
        log.warn("Token de rafraîchissement invalide");
        throw new RuntimeException("Token de rafraîchissement invalide");
      }

      // Génération de nouveaux tokens
      String newToken = jwtService.generateToken(userDetails);
      String newRefreshToken = jwtService.generateRefreshToken(userDetails);

      LoginResponse response = new LoginResponse();
      response.setToken(newToken);
      response.setRefreshToken(newRefreshToken);
      response.setUser(LoginResponse.UserDto.from(user));

      log.info("Token rafraîchi avec succès pour l'utilisateur: {}", user.getEmail());
      return response;

    } catch (Exception e) {
      log.warn("Erreur lors du rafraîchissement du token: {}", e.getMessage());
      throw new RuntimeException("Token de rafraîchissement invalide");
    }
  }

  /** Classe interne pour implémenter UserDetails */
  private static class CustomUserDetails
      implements org.springframework.security.core.userdetails.UserDetails {
    private final User user;

    public CustomUserDetails(User user) {
      this.user = user;
    }

    @Override
    public String getUsername() {
      return user.getUsername();
    }

    @Override
    public String getPassword() {
      return ""; // Pas de mot de passe pour le MVP
    }

    @Override
    public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority>
        getAuthorities() {
      return java.util.List.of(
          new org.springframework.security.core.authority.SimpleGrantedAuthority(
              "ROLE_" + user.getRole().name()));
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
