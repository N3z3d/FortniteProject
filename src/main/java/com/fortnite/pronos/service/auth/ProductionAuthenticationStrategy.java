package com.fortnite.pronos.service.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.core.env.Environment;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.domain.port.out.UserRepositoryPort;
import com.fortnite.pronos.dto.auth.LoginRequest;
import com.fortnite.pronos.dto.auth.LoginResponse;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.util.AuditLogger;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Production authentication strategy with full security features Used in production and test
 * environments
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductionAuthenticationStrategy implements AuthenticationStrategy {

  private static final String INVALID_CREDENTIALS_MESSAGE = "Email ou mot de passe incorrect";
  private static final String USER_NOT_FOUND_MESSAGE = "Utilisateur non trouvé";
  private static final String INVALID_TOKEN_MESSAGE = "Token invalide";
  private static final String INVALID_REFRESH_TOKEN_MESSAGE = "Token de rafraîchissement invalide";

  private final UserRepositoryPort userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuditLogger auditLogger;
  private final Environment environment;

  @Override
  public boolean isActive() {
    return !Arrays.asList(environment.getActiveProfiles()).contains("dev");
  }

  /** Génère la clé de signature pour les tokens JWT */
  private SecretKey getSigningKey() {
    try {
      byte[] keyBytes = "ma_super_cle_jwt_123456789_tres_longue_pour_securite".getBytes();
      return Keys.hmacShaKeyFor(keyBytes);
    } catch (Exception e) {
      log.error("Erreur lors de la génération de la clé de signature JWT", e);
      auditLogger.logSystemError(
          "ProductionAuthStrategy", "JWT_KEY_GENERATION_ERROR", e.getMessage());
      throw new IllegalStateException("Configuration JWT invalide");
    }
  }

  @Override
  @Transactional(readOnly = true)
  public LoginResponse login(LoginRequest request) {
    log.info("Tentative de connexion pour l'email: {}", request.getEmail());

    try {
      User user =
          userRepository
              .findByEmail(request.getEmail())
              .orElseThrow(
                  () -> {
                    log.warn(
                        "Tentative de connexion avec un email inexistant: {}", request.getEmail());
                    auditLogger.logFailedLogin(request.getEmail(), "EMAIL_NOT_FOUND");
                    return new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
                  });

      if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
        log.warn(
            "Tentative de connexion avec un mot de passe incorrect pour l'utilisateur: {}",
            user.getId());
        auditLogger.logFailedLogin(request.getEmail(), "INVALID_PASSWORD");
        throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
      }

      String token = generateToken(user);
      String refreshToken = generateRefreshToken(user);

      log.info("Connexion réussie pour l'utilisateur: {} ({})", user.getId(), user.getEmail());
      auditLogger.logUserLogin(user.getId(), user.getEmail(), true);

      LoginResponse response = new LoginResponse();
      response.setToken(token);
      response.setRefreshToken(refreshToken);
      response.setUser(LoginResponse.UserDto.from(user));

      return response;
    } catch (BadCredentialsException e) {
      log.warn("Échec de connexion pour l'email: {}", request.getEmail());
      throw e;
    } catch (Exception e) {
      log.error("Erreur inattendue lors de la connexion pour l'email: {}", request.getEmail(), e);
      auditLogger.logSystemError(
          "ProductionAuthStrategy", "LOGIN_UNEXPECTED_ERROR", e.getMessage());
      throw new RuntimeException("Erreur interne lors de la connexion");
    }
  }

  @Override
  @Transactional(readOnly = true)
  public LoginResponse refreshToken(String refreshToken) {
    log.debug("Tentative de rafraîchissement de token");

    try {
      Claims claims = validateToken(refreshToken);
      UUID userId = UUID.fromString(claims.getSubject());

      User user =
          userRepository
              .findById(userId)
              .orElseThrow(
                  () -> {
                    log.warn(
                        "Tentative de rafraîchissement avec un utilisateur inexistant: {}", userId);
                    auditLogger.logSystemError(
                        "ProductionAuthStrategy",
                        "REFRESH_TOKEN_USER_NOT_FOUND",
                        userId.toString());
                    return new BadCredentialsException(USER_NOT_FOUND_MESSAGE);
                  });

      String newToken = generateToken(user);
      String newRefreshToken = generateRefreshToken(user);

      log.info("Token rafraîchi avec succès pour l'utilisateur: {}", userId);
      auditLogger.logUserAction(userId, "TOKEN_REFRESH", "Token refreshed successfully");

      LoginResponse response = new LoginResponse();
      response.setToken(newToken);
      response.setRefreshToken(newRefreshToken);
      response.setUser(LoginResponse.UserDto.from(user));

      return response;
    } catch (BadCredentialsException e) {
      throw e;
    } catch (Exception e) {
      log.warn("Échec du rafraîchissement de token: {}", e.getMessage());
      auditLogger.logSystemError("ProductionAuthStrategy", "REFRESH_TOKEN_ERROR", e.getMessage());
      throw new BadCredentialsException(INVALID_REFRESH_TOKEN_MESSAGE);
    }
  }

  /** Génère un token d'accès JWT pour un utilisateur */
  private String generateToken(User user) {
    try {
      Map<String, Object> claims = createUserClaims(user);

      return Jwts.builder()
          .claims(claims)
          .subject(user.getId().toString())
          .issuedAt(Date.from(Instant.now()))
          .expiration(Date.from(Instant.now().plus(86400000, ChronoUnit.MILLIS)))
          .signWith(getSigningKey())
          .compact();
    } catch (Exception e) {
      log.error("Erreur lors de la génération du token pour l'utilisateur: {}", user.getId(), e);
      auditLogger.logSystemError(
          "ProductionAuthStrategy",
          "TOKEN_GENERATION_ERROR",
          String.format("UserId: %s, Error: %s", user.getId(), e.getMessage()));
      throw new RuntimeException("Erreur lors de la génération du token");
    }
  }

  /** Génère un refresh token JWT pour un utilisateur */
  private String generateRefreshToken(User user) {
    try {
      return Jwts.builder()
          .subject(user.getId().toString())
          .issuedAt(Date.from(Instant.now()))
          .expiration(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)))
          .signWith(getSigningKey())
          .compact();
    } catch (Exception e) {
      log.error(
          "Erreur lors de la génération du refresh token pour l'utilisateur: {}", user.getId(), e);
      auditLogger.logSystemError(
          "ProductionAuthStrategy",
          "REFRESH_TOKEN_GENERATION_ERROR",
          String.format("UserId: %s, Error: %s", user.getId(), e.getMessage()));
      throw new RuntimeException("Erreur lors de la génération du refresh token");
    }
  }

  /** Crée les claims pour un token JWT */
  private Map<String, Object> createUserClaims(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("role", user.getRole().name());
    claims.put("email", user.getEmail());
    claims.put("userId", user.getId().toString());
    return claims;
  }

  /** Valide un token JWT et retourne les claims */
  private Claims validateToken(String token) {
    try {
      return Jwts.parser()
          .verifyWith(getSigningKey())
          .build()
          .parseSignedClaims(token)
          .getPayload();
    } catch (SignatureException e) {
      log.warn("Token avec signature invalide: {}", e.getMessage());
      throw new BadCredentialsException(INVALID_TOKEN_MESSAGE);
    } catch (Exception e) {
      log.warn("Token invalide: {}", e.getMessage());
      throw new BadCredentialsException(INVALID_TOKEN_MESSAGE);
    }
  }
}
