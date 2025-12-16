package com.fortnite.pronos.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.fortnite.pronos.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service d'authentification flexible pour le MVP */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlexibleAuthenticationService {

  private final UnifiedAuthService unifiedAuthService;
  private final Environment environment;

  /** Authentifie un utilisateur de maniere flexible */
  public Optional<User> authenticate(String username) {
    return unifiedAuthService.authenticate(username);
  }

  /** Verifie si un utilisateur existe */
  public boolean userExists(String username) {
    return unifiedAuthService.findUserByUsername(username).isPresent();
  }

  /** Recupere l'utilisateur actuel depuis le contexte de securite */
  public User getCurrentUser() {
    log.info("Recuperation de l'utilisateur actuel");

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      log.warn("Aucun utilisateur authentifie trouve");
      // Fallback uniquement en dev/local, jamais en profil de test
      if (isDevelopmentEnvironment()) {
        log.info("Profil de developpement detecte: utilisation d'un utilisateur par defaut");
        return createDefaultUser();
      }
      throw new RuntimeException("Utilisateur non authentifie");
    }

    String username = authentication.getName();
    log.info("Username depuis l'authentification: {}", username);

    // Pour le MVP, on utilise une strategie simplifiee
    if ("anonymousUser".equals(username) || username == null) {
      log.info("Utilisateur anonyme detecte, retour d'un utilisateur par defaut");
      return createDefaultUser();
    }

    Optional<User> userOpt = unifiedAuthService.findUserByUsername(username);

    if (userOpt.isEmpty()) {
      log.warn("Utilisateur authentifie mais non trouve en base: {}", username);
      return createTemporaryUser(username);
    }

    User user = userOpt.get();
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
    if (datasourceUrl.contains("h2")) {
      return "H2 (In-Memory)";
    } else if (datasourceUrl.contains("postgresql")) {
      return "PostgreSQL";
    } else if (datasourceUrl.contains("mysql")) {
      return "MySQL";
    } else {
      return "Unknown";
    }
  }

  /** Cree un utilisateur par defaut pour le MVP */
  private User createDefaultUser() {
    User defaultUser = new User();
    defaultUser.setId(java.util.UUID.nameUUIDFromBytes("dev-user".getBytes()));
    defaultUser.setEmail("dev@fortnite-pronos.com");
    defaultUser.setUsername("dev-user");
    defaultUser.setPassword("password");
    defaultUser.setCurrentSeason(2025);
    defaultUser.setRole(User.UserRole.ADMIN);

    log.info("Utilisateur par defaut cree: {}", defaultUser.getEmail());

    return defaultUser;
  }

  /** Cree un utilisateur temporaire pour le MVP */
  private User createTemporaryUser(String username) {
    User tempUser = new User();
    tempUser.setEmail(username + "@temp.com");
    tempUser.setUsername(username);
    tempUser.setRole(User.UserRole.USER);

    log.info("Utilisateur temporaire cree: {}", tempUser.getEmail());

    return tempUser;
  }
}
