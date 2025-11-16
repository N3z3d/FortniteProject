package com.fortnite.pronos.service;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.exception.UserNotFoundException;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service pour récupérer l'utilisateur courant depuis le contexte de sécurité */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserContextService {

  private final UserRepository userRepository;
  private final UnifiedAuthService unifiedAuthService;

  /**
   * Récupère l'ID de l'utilisateur courant
   *
   * @return UUID de l'utilisateur authentifié
   * @throws IllegalStateException si aucun utilisateur n'est authentifié
   */
  public UUID getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      log.warn("Aucun utilisateur authentifié trouvé dans le contexte de sécurité");
      throw new IllegalStateException("Utilisateur non authentifié");
    }

    String username = authentication.getName();
    log.debug("Récupération de l'utilisateur courant: {}", username);

    User user =
        userRepository
            .findByUsernameIgnoreCase(username)
            .orElseThrow(
                () -> {
                  log.error("Utilisateur non trouvé en base: {}", username);
                  return new IllegalStateException("Utilisateur non trouvé: " + username);
                });

    return user.getId();
  }

  /**
   * Récupère l'utilisateur courant complet
   *
   * @return User authentifié
   * @throws IllegalStateException si aucun utilisateur n'est authentifié
   */
  public User getCurrentUser() {
    UUID userId = getCurrentUserId();
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé: " + userId));
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
      log.error("Paramètre username manquant - authentification requise");
      throw new IllegalStateException("Paramètre username requis pour l'authentification");
    }

    final String finalUsername = usernameParam.trim();
    log.debug("Récupération de l'utilisateur depuis le paramètre: {}", finalUsername);

    User user =
        userRepository
            .findByUsernameIgnoreCase(finalUsername)
            .orElseThrow(
                () -> {
                  log.error("Utilisateur non trouvé pour le paramètre: {}", finalUsername);
                  return new IllegalStateException("Utilisateur non trouvé: " + finalUsername);
                });

    return user;
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
      return authentication != null && authentication.isAuthenticated();
    } catch (Exception e) {
      log.warn("Erreur lors de la vérification de l'authentification", e);
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

    return userRepository
        .findByUsernameIgnoreCase(username)
        .orElseThrow(
            () -> {
              log.error("Utilisateur non trouvé en base: {}", username);
              return new UserNotFoundException("Utilisateur non trouvé: " + username);
            });
  }

  /**
   * Récupère l'ID d'un utilisateur par son nom d'utilisateur
   *
   * @param username nom d'utilisateur
   * @return UUID de l'utilisateur
   * @throws UserNotFoundException si l'utilisateur n'est pas trouvé
   */
  public UUID getUserIdFromUsername(String username) {
    log.debug("Récupération de l'ID utilisateur par nom: {}", username);

    User user =
        userRepository
            .findByUsernameIgnoreCase(username)
            .orElseThrow(
                () -> {
                  log.error("Utilisateur non trouvé en base: {}", username);
                  return new UserNotFoundException("Utilisateur non trouvé: " + username);
                });

    return user.getId();
  }
}
