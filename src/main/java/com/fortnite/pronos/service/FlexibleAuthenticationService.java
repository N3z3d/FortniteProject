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

  /** Authentifie un utilisateur de manière flexible */
  public Optional<User> authenticate(String username) {
    return unifiedAuthService.authenticate(username);
  }

  /** Vérifie si un utilisateur existe */
  public boolean userExists(String username) {
    return unifiedAuthService.findUserByUsername(username).isPresent();
  }

  /** Récupère l'utilisateur actuel depuis le contexte de sécurité */
  public User getCurrentUser() {
    log.info("Récupération de l'utilisateur actuel");

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      log.warn("Aucun utilisateur authentifié trouvé");
      throw new RuntimeException("Utilisateur non authentifié");
    }

    String username = authentication.getName();
    log.info("Username depuis l'authentification: {}", username);

    // Pour le MVP, on utilise une stratégie simplifiée
    // En cas d'anonyme ou de username spécial, on retourne un utilisateur par défaut
    if ("anonymousUser".equals(username) || username == null) {
      log.info("Utilisateur anonyme détecté, retour d'un utilisateur par défaut");
      return createDefaultUser();
    }

    Optional<User> userOpt = unifiedAuthService.findUserByUsername(username);

    if (userOpt.isEmpty()) {
      log.warn("Utilisateur authentifié mais non trouvé en base: {}", username);
      // Pour le MVP, on crée un utilisateur temporaire
      return createTemporaryUser(username);
    }

    User user = userOpt.get();
    log.info("Utilisateur actuel récupéré: {} ({})", user.getEmail(), user.getRole());

    return user;
  }

  /** Récupère les informations de l'environnement */
  public Map<String, Object> getEnvironmentInfo() {
    log.info("Récupération des informations de l'environnement");

    Map<String, Object> info = new HashMap<>();

    // Profils actifs
    String[] activeProfiles = environment.getActiveProfiles();
    info.put(
        "activeProfiles", activeProfiles.length > 0 ? activeProfiles : new String[] {"default"});

    // Environnement de développement
    boolean isDev = isDevelopmentEnvironment();
    info.put("isDevelopment", isDev);

    // Version de l'application
    info.put(
        "applicationName", environment.getProperty("spring.application.name", "fortnite-pronos"));
    info.put("version", environment.getProperty("app.version", "0.1.0-SNAPSHOT"));

    // Base de données
    String datasourceUrl = environment.getProperty("spring.datasource.url", "Unknown");
    info.put("database", getDatabaseType(datasourceUrl));

    // Configuration JWT
    info.put("jwtEnabled", environment.getProperty("jwt.enabled", "true"));

    // Informations sur l'authentification
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    info.put("authenticated", auth != null && auth.isAuthenticated());
    info.put("principal", auth != null ? auth.getName() : "anonymous");

    log.info("Informations de l'environnement récupérées: {}", info);

    return info;
  }

  /** Vérifie si on est en environnement de développement */
  private boolean isDevelopmentEnvironment() {
    String[] activeProfiles = environment.getActiveProfiles();

    for (String profile : activeProfiles) {
      if ("dev".equals(profile) || "development".equals(profile) || "h2".equals(profile)) {
        return true;
      }
    }

    return false;
  }

  /** Détermine le type de base de données depuis l'URL */
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

  /** Crée un utilisateur par défaut pour le MVP */
  private User createDefaultUser() {
    User defaultUser = new User();
    defaultUser.setEmail("dev@fortnite-pronos.com");
    defaultUser.setUsername("dev-user");
    defaultUser.setRole(User.UserRole.ADMIN);

    log.info("Utilisateur par défaut créé: {}", defaultUser.getEmail());

    return defaultUser;
  }

  /** Crée un utilisateur temporaire pour le MVP */
  private User createTemporaryUser(String username) {
    User tempUser = new User();
    tempUser.setEmail(username + "@temp.com");
    tempUser.setUsername(username);
    tempUser.setRole(User.UserRole.PARTICIPANT);

    log.info("Utilisateur temporaire créé: {}", tempUser.getEmail());

    return tempUser;
  }
}
