package com.fortnite.pronos.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * Service de gestion des tokens JWT optimisé pour les performances
 *
 * <p>Optimisations: - Mise en cache de la SecretKey pour éviter la re-création - Validation de
 * configuration au démarrage pour un échec rapide - Configuration par défaut sécurisée pour le
 * développement - Lazy loading avec thread-safety
 */
@Service
@Slf4j
public class JwtService {

  @Value("${app.jwt.secret:#{null}}")
  private String secretKey;

  @Value("${app.jwt.expiration:86400000}")
  private Long jwtExpiration;

  @Value("${app.jwt.refresh-expiration:604800000}")
  private Long refreshExpiration;

  private final Environment environment;

  // Performance: Cache de la SecretKey pour éviter les re-créations
  private volatile SecretKey cachedSecretKey;
  private final Object secretKeyLock = new Object();

  public JwtService(Environment environment) {
    this.environment = environment;
  }

  /**
   * Initialisation et validation de la configuration JWT au démarrage Performance: Échec rapide au
   * lieu d'attendre la première utilisation Security: PRODUCTION SECURITY - Application fails to
   * start if JWT_SECRET not configured in production
   */
  @PostConstruct
  private void initializeJwtConfiguration() {
    // Skip initialization during tests when environment is null
    if (environment == null) {
      log.debug("Skipping JWT initialization - environment is null (test context)");
      return;
    }

    log.info("Initialisation de la configuration JWT...");

    // SECURITY CRITICAL: Validate production JWT configuration
    validateProductionSecurity();

    // Performance: Valider et initialiser la clé au démarrage
    try {
      getSignInKey(); // Force la création et validation
      log.info("Configuration JWT initialisée avec succès");
    } catch (Exception e) {
      log.error("Échec de l'initialisation JWT: {}", e.getMessage());
      throw e; // Échec rapide au démarrage
    }
  }

  /**
   * SECURITY CRITICAL: Validate JWT configuration for production deployment Application will fail
   * to start if JWT_SECRET is not properly configured in production
   */
  private void validateProductionSecurity() {
    String[] activeProfiles = environment.getActiveProfiles();
    boolean isProduction = isProductionEnvironment(activeProfiles);

    log.info(
        "Environment check - Active profiles: {}, Production: {}",
        String.join(", ", activeProfiles),
        isProduction);

    if (isProduction) {
      // PRODUCTION SECURITY: JWT_SECRET must be configured via environment variable
      String envJwtSecret = System.getenv("JWT_SECRET");

      if (envJwtSecret == null || envJwtSecret.trim().isEmpty()) {
        String errorMsg =
            "PRODUCTION SECURITY ERROR: JWT_SECRET environment variable is not configured. "
                + "Application cannot start in production without a secure JWT secret. "
                + "Please set JWT_SECRET environment variable with a strong 256-bit secret.";
        log.error(errorMsg);
        throw new IllegalStateException(errorMsg);
      }

      if (envJwtSecret.length() < 64) {
        String errorMsg =
            "PRODUCTION SECURITY ERROR: JWT_SECRET is too short for production. "
                + "Minimum 64 characters required for production security. "
                + "Current length: "
                + envJwtSecret.length();
        log.error(errorMsg);
        throw new IllegalStateException(errorMsg);
      }

      log.info("✅ Production JWT security validation passed - JWT_SECRET properly configured");
    } else {
      log.info("Development/Test environment detected - using development JWT fallback");
    }
  }

  /** Determine if the current environment is production */
  private boolean isProductionEnvironment(String[] activeProfiles) {
    for (String profile : activeProfiles) {
      if ("prod".equals(profile) || "production".equals(profile)) {
        return true;
      }
    }
    return false;
  }

  /** Extraire le nom d'utilisateur du token */
  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  /** Extraire une claim spécifique du token */
  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  /** Générer un token pour un utilisateur */
  public String generateToken(UserDetails userDetails) {
    return generateToken(new HashMap<>(), userDetails);
  }

  /** Générer un token avec des claims personnalisées */
  public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
    return buildToken(extraClaims, userDetails, jwtExpiration);
  }

  /** Générer un token de refresh */
  public String generateRefreshToken(UserDetails userDetails) {
    return buildToken(new HashMap<>(), userDetails, refreshExpiration);
  }

  /** Vérifier si le token est valide */
  public boolean isTokenValid(String token, UserDetails userDetails) {
    try {
      final String username = extractUsername(token);
      return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    } catch (Exception e) {
      log.debug("Invalid JWT token: {}", e.getMessage());
      return false;
    }
  }

  /** Vérifier si le token a expiré */
  private boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  /** Extraire la date d'expiration du token */
  private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  /** Extraire toutes les claims du token */
  private Claims extractAllClaims(String token) {
    try {
      return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
    } catch (Exception e) {
      log.debug("Failed to parse JWT claims: {}", e.getMessage());
      throw new RuntimeException("Invalid JWT token", e);
    }
  }

  /** Construire un token */
  private String buildToken(
      Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
    return Jwts.builder()
        .setClaims(extraClaims)
        .setSubject(userDetails.getUsername())
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSignInKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  /**
   * Obtenir la clé de signature avec mise en cache pour les performances Performance: ~0.1-0.5ms
   * économisés par opération JWT
   */
  private SecretKey getSignInKey() {
    // Performance: Double-checked locking pour thread-safety avec minimum de synchronisation
    if (cachedSecretKey == null) {
      synchronized (secretKeyLock) {
        if (cachedSecretKey == null) {
          cachedSecretKey = createSecretKey();
        }
      }
    }
    return cachedSecretKey;
  }

  /**
   * Créer la SecretKey avec fallback sécurisé pour le développement Performance: Appelé une seule
   * fois grâce au cache
   */
  private SecretKey createSecretKey() {
    String effectiveSecret = resolveJwtSecret();

    // Security: Assurer une clé suffisamment longue pour HMAC (minimum 32 caractères)
    if (effectiveSecret.length() < 32) {
      log.error(
          "JWT secret trop court - minimum 32 caractères requis, longueur actuelle: {}",
          effectiveSecret.length());
      throw new IllegalStateException("JWT secret trop court - minimum 32 caractères requis");
    }

    log.debug("Création de la SecretKey JWT (longueur: {} caractères)", effectiveSecret.length());
    byte[] keyBytes = effectiveSecret.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  /**
   * Résoudre le secret JWT avec priorité à la variable d'environnement Performance: Configuration
   * par défaut évite les échecs de démarrage Security: Environment variable JWT_SECRET takes
   * priority over application properties
   */
  private String resolveJwtSecret() {
    // SECURITY PRIORITY 1: Environment variable JWT_SECRET (production recommended)
    String envJwtSecret = System.getenv("JWT_SECRET");
    if (envJwtSecret != null && !envJwtSecret.trim().isEmpty()) {
      log.debug("Utilisation du secret JWT depuis variable d'environnement JWT_SECRET");
      return envJwtSecret.trim();
    }

    // SECURITY PRIORITY 2: Application properties configuration
    if (secretKey != null && !secretKey.trim().isEmpty()) {
      log.debug("Utilisation du secret JWT configuré via application properties");
      return secretKey.trim();
    }

    // DEVELOPMENT FALLBACK: Only for local development
    String devSecret =
        "development-jwt-secret-key-change-in-production-this-is-only-for-local-dev-work-minimum-32-chars";
    log.warn(
        "⚠️  ATTENTION: Utilisation du secret JWT par défaut pour le développement. "
            + "Configurez JWT_SECRET en production via variable d'environnement!");
    return devSecret;
  }
}
