package com.fortnite.pronos.service;

import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.exception.UserNotFoundException;
import com.fortnite.pronos.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service pour récupérer l'utilisateur courant depuis le contexte de sécurité */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserContextService {

  private final UserService userService;

  /**
   * Récupère l'ID de l'utilisateur courant
   *
   * @return UUID de l'utilisateur authentifié
   * @throws IllegalStateException si aucun utilisateur n'est authentifié
   */
  public UUID getCurrentUserId() {
    String username = resolveAuthenticatedUsername();
    log.debug("Recuperation de l'utilisateur courant: {}", username);
    return getUserOrIllegalState(username).getId();
  }

  /**
   * Récupère l'utilisateur courant complet
   *
   * @return User authentifié
   * @throws IllegalStateException si aucun utilisateur n'est authentifié
   */
  public User getCurrentUser() {
    UUID userId = getCurrentUserId();
    return userService
        .findUserById(userId)
        .orElseThrow(() -> new IllegalStateException("Utilisateur non trouve: " + userId));
  }

  /**
   * Récupère l'utilisateur courant depuis un paramètre de requête (mode développement)
   *
   * @param usernameParam nom d'utilisateur depuis le paramètre de requête
   * @return User correspondant
   * @throws IllegalStateException si l'utilisateur n'est pas trouvé
   */
  public User getCurrentUserFromParam(String usernameParam) {
    if (usernameParam == null || usernameParam.trim().isEmpty()) {
      log.error("Parametre username manquant - authentification requise");
      throw new IllegalStateException("Parametre username requis pour l'authentification");
    }

    String sanitizedUsername = usernameParam.trim();
    log.debug("Recuperation de l'utilisateur depuis le parametre: {}", sanitizedUsername);

    return getUserOrIllegalState(sanitizedUsername);
  }

  /**
   * Récupère l'ID de l'utilisateur courant depuis un paramètre de requête
   *
   * @param usernameParam nom d'utilisateur depuis le paramètre de requête
   * @return UUID de l'utilisateur
   * @throws IllegalStateException si l'utilisateur n'est pas trouvé
   */
  public UUID getCurrentUserIdFromParam(String usernameParam) {
    return getCurrentUserFromParam(usernameParam).getId();
  }

  /**
   * Vérifie si un utilisateur est authentifié
   *
   * @return true si un utilisateur est authentifié
   */
  public boolean isUserAuthenticated() {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null) {
        return false;
      }
      if (!authentication.isAuthenticated()) {
        return false;
      }
      if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
        return false;
      }
      String username = authentication.getName();
      return username != null && !username.trim().isEmpty();
    } catch (Exception e) {
      log.warn("Erreur lors de la verification de l'authentification", e);
      return false;
    }
  }

  /**
   * Récupère l'utilisateur courant avec fallback vers le paramètre de requête Clean Code :
   * Privilégie le paramètre user quand fourni, sinon utilise Spring Security
   *
   * @param usernameParam nom d'utilisateur depuis le paramètre de requête (optionnel)
   * @return User authentifié
   * @throws IllegalStateException si aucun utilisateur n'est trouvé
   */
  public User getCurrentUserWithFallback(String usernameParam) {
    // Privilégier le paramètre user s'il est fourni
    if (usernameParam != null && !usernameParam.trim().isEmpty()) {
      log.debug("Utilisation du paramètre user: {}", usernameParam);
      return getCurrentUserFromParam(usernameParam);
    }

    // Sinon, utiliser Spring Security si disponible
    if (isUserAuthenticated()) {
      return getCurrentUser();
    } else {
      // Aucun utilisateur authentifié - erreur de sécurité
      log.error("Aucun utilisateur authentifié et aucun paramètre user fourni");
      throw new IllegalStateException("Authentification requise - aucun utilisateur connecté");
    }
  }

  /**
   * Récupère l'ID de l'utilisateur courant avec fallback vers le paramètre de requête
   *
   * @param usernameParam nom d'utilisateur depuis le paramètre de requête (optionnel)
   * @return UUID de l'utilisateur
   * @throws IllegalStateException si aucun utilisateur n'est trouvé
   */
  public UUID getCurrentUserIdWithFallback(String usernameParam) {
    return getCurrentUserWithFallback(usernameParam).getId();
  }

  /**
   * Trouve un utilisateur par son nom d'utilisateur Clean Code : méthode simple avec une
   * responsabilité unique
   */
  public User findUserByUsername(String username) {
    log.debug("Recherche de l'utilisateur par nom: {}", username);

    return getUserOrNotFound(username);
  }

  /**
   * Récupère l'ID d'un utilisateur par son nom d'utilisateur
   *
   * @param username nom d'utilisateur
   * @return UUID de l'utilisateur
   * @throws UserNotFoundException si l'utilisateur n'est pas trouvé
   */
  public UUID getUserIdFromUsername(String username) {
    log.debug("Recuperation de l'ID utilisateur par nom: {}", username);

    User user = getUserOrNotFound(username);

    return user.getId();
  }

  private String resolveAuthenticatedUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      log.warn("Aucun utilisateur authentifie trouve dans le contexte de securite");
      throw new IllegalStateException("Utilisateur non authentifie");
    }

    if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
      log.warn("Authentification anonyme detectee, refus de continuer");
      throw new IllegalStateException("Utilisateur non authentifie");
    }

    String username = authentication.getName();
    if (username == null || username.trim().isEmpty()) {
      log.warn("Nom d'utilisateur manquant dans le contexte de securite");
      throw new IllegalStateException("Nom d'utilisateur manquant dans le contexte de securite");
    }

    return username.trim();
  }

  private User getUserOrIllegalState(String username) {
    return userService
        .findUserByUsername(username)
        .orElseThrow(
            () -> {
              log.error("Utilisateur non trouve en base: {}", username);
              return new IllegalStateException("Utilisateur non trouve: " + username);
            });
  }

  private User getUserOrNotFound(String username) {
    return userService
        .findUserByUsername(username)
        .orElseThrow(
            () -> {
              log.error("Utilisateur non trouve en base: {}", username);
              return new UserNotFoundException("Utilisateur non trouve: " + username);
            });
  }
}






