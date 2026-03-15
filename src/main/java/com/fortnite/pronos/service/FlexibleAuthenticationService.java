package com.fortnite.pronos.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service d'authentification flexible pour le MVP */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"java:S112"})
public class FlexibleAuthenticationService {

  private static final int DEFAULT_SEASON = 2025;
  private static final String ANONYMOUS_USER = "anonymousUser";

  private final UnifiedAuthService unifiedAuthService;
  private final Environment environment;

  /** Authentifie un utilisateur de maniere flexible */
  public Optional<com.fortnite.pronos.model.User> authenticate(String username) {
    return unifiedAuthService.authenticate(username);
  }

  /** Verifie si un utilisateur existe */
  public boolean userExists(String username) {
    return unifiedAuthService.findUserByUsername(username).isPresent();
  }

  /** Recupere l'utilisateur actuel depuis le contexte de securite */
  public com.fortnite.pronos.model.User getCurrentUser() {
    log.info("Recuperation de l'utilisateur actuel");

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      return resolveUnauthenticatedUser();
    }

    return resolveAuthenticatedUser(authentication.getName());
  }

  private com.fortnite.pronos.model.User resolveUnauthenticatedUser() {
    log.warn("Aucun utilisateur authentifie trouve");
    // Fallback uniquement en dev/local, jamais en profil de test
    if (isDevelopmentEnvironment()) {
      log.info("Profil de developpement detecte: utilisation d'un utilisateur par defaut");
      return createDefaultUser();
    }
    throw new RuntimeException("Utilisateur non authentifie");
  }

  private com.fortnite.pronos.model.User resolveAuthenticatedUser(String username) {
    log.info("Username depuis l'authentification: {}", username);

    // Pour le MVP, on utilise une strategie simplifiee
    if (ANONYMOUS_USER.equals(username) || username == null) {
      log.info("Utilisateur anonyme detecte, retour d'un utilisateur par defaut");
      return createDefaultUser();
    }

    Optional<com.fortnite.pronos.model.User> userOpt =
        unifiedAuthService.findUserByUsername(username);

    if (userOpt.isEmpty()) {
      log.warn("Utilisateur authentifie mais non trouve en base: {}", username);
      return createTemporaryUser(username);
    }

    com.fortnite.pronos.model.User user = userOpt.get();
    log.info("Utilisateur actuel recupere: {} ({})", user.getEmail(), user.getRole());

    return user;
  }

  /** Recupere les informations de l'environnement */
  public Map<String, Object> getEnvironmentInfo() {
    log.info("Recuperation des informations de l'environnement");

    Map<String, Object> info = new HashMap<>();

    String[] activeProfiles = environment.getActiveProfiles();
    info.put(
        "activeProfiles", activeProfiles.length > 0 ? activeProfiles : new String[] {"default"});

    boolean isDev = isDevelopmentEnvironment();
    info.put("isDevelopment", isDev);

    info.put(
        "applicationName", environment.getProperty("spring.application.name", "fortnite-pronos"));
    info.put("version", environment.getProperty("app.version", "0.1.0-SNAPSHOT"));

    String datasourceUrl = environment.getProperty("spring.datasource.url", "Unknown");
    info.put("database", getDatabaseType(datasourceUrl));

    info.put("jwtEnabled", environment.getProperty("jwt.enabled", "true"));

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    info.put("authenticated", auth != null && auth.isAuthenticated());
    info.put("principal", auth != null ? auth.getName() : "anonymous");

    log.info("Informations de l'environnement recuperees: {}", info);

    return info;
  }

  /** Verifie si on est en environnement de developpement */
  private boolean isDevelopmentEnvironment() {
    String[] activeProfiles = environment.getActiveProfiles();

    for (String profile : activeProfiles) {
      if ("dev".equals(profile) || "development".equals(profile) || "local".equals(profile)) {
        return true;
      }
    }

    return false;
  }

  /** Determine le type de base de donnees depuis l'URL */
  private String getDatabaseType(String datasourceUrl) {
    String databaseType = "Unknown";
    if (datasourceUrl.contains("h2")) {
      databaseType = "H2 (In-Memory)";
    } else if (datasourceUrl.contains("postgresql")) {
      databaseType = "PostgreSQL";
    } else if (datasourceUrl.contains("mysql")) {
      databaseType = "MySQL";
    }
    return databaseType;
  }

  /** Cree un utilisateur par defaut pour le MVP */
  private com.fortnite.pronos.model.User createDefaultUser() {
    com.fortnite.pronos.model.User defaultUser = new com.fortnite.pronos.model.User();
    defaultUser.setId(
        java.util.UUID.nameUUIDFromBytes(
            "dev-user".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    defaultUser.setEmail("dev@fortnite-pronos.com");
    defaultUser.setUsername("dev-user");
    defaultUser.setPassword("password");
    defaultUser.setCurrentSeason(DEFAULT_SEASON);
    defaultUser.setRole(com.fortnite.pronos.model.User.UserRole.ADMIN);

    log.info("Utilisateur par defaut cree: {}", defaultUser.getEmail());

    return defaultUser;
  }

  /** Cree un utilisateur temporaire pour le MVP */
  private com.fortnite.pronos.model.User createTemporaryUser(String username) {
    com.fortnite.pronos.model.User tempUser = new com.fortnite.pronos.model.User();
    tempUser.setEmail(username + "@temp.com");
    tempUser.setUsername(username);
    tempUser.setRole(com.fortnite.pronos.model.User.UserRole.USER);

    log.info("Utilisateur temporaire cree: {}", tempUser.getEmail());

    return tempUser;
  }
}
