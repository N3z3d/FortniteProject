package com.fortnite.pronos.core.error;

/**
 * Codes d'erreur standardisés pour l'application. Suit la nomenclature NASA-STD-8719.13 pour la
 * gestion des erreurs.
 *
 * <p>Format: [DOMAINE]_[TYPE]_[NUMERO] - DOMAINE: AUTH, TEAM, PLAYER, SCORE, TRADE, SYS - TYPE: VAL
 * (Validation), BUS (Business), SYS (System), SEC (Security) - NUMERO: 3 chiffres
 *
 * @author Fortnite Pronos Team
 * @version 1.0
 * @since 2025-06-09
 */
public enum ErrorCode {

  // === ERREURS D'AUTHENTIFICATION ===
  AUTH_VAL_001("AUTH_VAL_001", "Nom d'utilisateur requis"),
  AUTH_VAL_002("AUTH_VAL_002", "Mot de passe requis"),
  AUTH_VAL_003("AUTH_VAL_003", "Format email invalide"),
  AUTH_BUS_001("AUTH_BUS_001", "Identifiants incorrects"),
  AUTH_BUS_002("AUTH_BUS_002", "Compte utilisateur désactivé"),
  AUTH_BUS_003("AUTH_BUS_003", "Compte utilisateur verrouillé"),
  AUTH_SEC_001("AUTH_SEC_001", "Token JWT invalide"),
  AUTH_SEC_002("AUTH_SEC_002", "Token JWT expiré"),
  AUTH_SEC_003("AUTH_SEC_003", "Tentatives de connexion trop nombreuses"),
  AUTH_SEC_004("AUTH_SEC_004", "Format de token invalide"),
  AUTH_SYS_001("AUTH_SYS_001", "Service d'authentification indisponible"),

  // === ERREURS DE JOUEUR ===
  PLAYER_VAL_001("PLAYER_VAL_001", "Identifiant Epic Games invalide"),
  PLAYER_VAL_002("PLAYER_VAL_002", "Pseudo trop court (minimum 3 caractères)"),
  PLAYER_VAL_003("PLAYER_VAL_003", "Pseudo trop long (maximum 50 caractères)"),
  PLAYER_BUS_001("PLAYER_BUS_001", "Joueur non trouvé"),
  PLAYER_BUS_002("PLAYER_BUS_002", "Joueur déjà existant"),
  PLAYER_BUS_003("PLAYER_BUS_003", "Joueur non actif pour cette saison"),

  // === ERREURS D'ÉQUIPE ===
  TEAM_VAL_001("TEAM_VAL_001", "Nom d'équipe requis"),
  TEAM_VAL_002("TEAM_VAL_002", "Saison invalide"),
  TEAM_VAL_003("TEAM_VAL_003", "Position de joueur invalide"),
  TEAM_BUS_001("TEAM_BUS_001", "Équipe non trouvée"),
  TEAM_BUS_002("TEAM_BUS_002", "Équipe déjà existante pour cette saison"),
  TEAM_BUS_003("TEAM_BUS_003", "Équipe complète (5 joueurs maximum)"),
  TEAM_BUS_004("TEAM_BUS_004", "Joueur déjà dans une équipe pour cette saison"),
  TEAM_BUS_005("TEAM_BUS_005", "Changement d'équipe non autorisé après date limite"),

  // === ERREURS DE SCORE ===
  SCORE_VAL_001("SCORE_VAL_001", "Points invalides (doivent être >= 0)"),
  SCORE_VAL_002("SCORE_VAL_002", "Date de score invalide"),
  SCORE_BUS_001("SCORE_BUS_001", "Score non trouvé"),
  SCORE_BUS_002("SCORE_BUS_002", "Score déjà enregistré pour cette période"),
  SCORE_BUS_003("SCORE_BUS_003", "Modification de score non autorisée"),

  // === ERREURS D'ÉCHANGE ===
  TRADE_VAL_001("TRADE_VAL_001", "Joueur proposé requis"),
  TRADE_VAL_002("TRADE_VAL_002", "Joueur demandé requis"),
  TRADE_BUS_001("TRADE_BUS_001", "Échange non trouvé"),
  TRADE_BUS_002("TRADE_BUS_002", "Échange déjà accepté ou refusé"),
  TRADE_BUS_003("TRADE_BUS_003", "Auto-échange non autorisé"),
  TRADE_BUS_004("TRADE_BUS_004", "Limite d'échanges atteinte pour cette saison"),
  TRADE_BUS_005("TRADE_BUS_005", "Période d'échange fermée"),

  // === ERREURS SYSTÈME ===
  SYS_001("SYS_001", "Erreur interne du serveur"),
  SYS_002("SYS_002", "Service temporairement indisponible"),
  SYS_003("SYS_003", "Timeout de la base de données"),
  SYS_004("SYS_004", "Limite de taux dépassée"),
  SYS_005("SYS_005", "Configuration manquante ou invalide"),

  // === ERREURS GÉNÉRIQUES ===
  INVALID_TOKEN_FORMAT("AUTH_SEC_004", "Format de token invalide"),
  AUTHENTICATION_SERVICE_UNAVAILABLE("AUTH_SYS_001", "Service d'authentification indisponible");

  private final String code;
  private final String defaultMessage;

  ErrorCode(String code, String defaultMessage) {
    this.code = code;
    this.defaultMessage = defaultMessage;
  }

  public String getCode() {
    return code;
  }

  public String getDefaultMessage() {
    return defaultMessage;
  }

  /**
   * Récupère le domaine métier de l'erreur.
   *
   * @return le domaine (AUTH, TEAM, PLAYER, etc.)
   */
  public String getDomain() {
    return code.split("_")[0];
  }

  /**
   * Récupère le type d'erreur.
   *
   * @return le type (VAL, BUS, SYS, SEC)
   */
  public String getType() {
    return code.split("_")[1];
  }

  /**
   * Indique si c'est une erreur de validation.
   *
   * @return true si c'est une erreur de validation
   */
  public boolean isValidationError() {
    return getType().equals("VAL");
  }

  /**
   * Indique si c'est une erreur métier.
   *
   * @return true si c'est une erreur métier
   */
  public boolean isBusinessError() {
    return getType().equals("BUS");
  }

  /**
   * Indique si c'est une erreur système.
   *
   * @return true si c'est une erreur système
   */
  public boolean isSystemError() {
    return getType().equals("SYS") || code.startsWith("SYS_");
  }

  /**
   * Indique si c'est une erreur de sécurité.
   *
   * @return true si c'est une erreur de sécurité
   */
  public boolean isSecurityError() {
    return getType().equals("SEC");
  }

  /**
   * Récupère le code de statut HTTP correspondant à l'erreur.
   *
   * <p>Uses standard HTTP status codes (framework-agnostic): - 400: Bad Request - 401: Unauthorized
   * - 403: Forbidden - 404: Not Found - 409: Conflict - 429: Too Many Requests - 500: Internal
   * Server Error - 503: Service Unavailable
   *
   * @return le code de statut HTTP approprié
   */
  public int getStatusCode() {
    if (isValidationError()) {
      return 400; // BAD_REQUEST
    }

    switch (this) {
      case AUTH_BUS_001:
      case AUTH_SEC_001:
      case AUTH_SEC_002:
      case AUTH_SEC_004:
      case INVALID_TOKEN_FORMAT:
        return 401; // UNAUTHORIZED

      case AUTH_BUS_002:
      case AUTH_BUS_003:
      case AUTH_SEC_003:
        return 403; // FORBIDDEN

      case PLAYER_BUS_001:
      case TEAM_BUS_001:
      case SCORE_BUS_001:
      case TRADE_BUS_001:
        return 404; // NOT_FOUND

      case PLAYER_BUS_002:
      case TEAM_BUS_002:
      case SCORE_BUS_002:
        return 409; // CONFLICT

      case SYS_002:
      case AUTH_SYS_001:
      case AUTHENTICATION_SERVICE_UNAVAILABLE:
        return 503; // SERVICE_UNAVAILABLE

      case SYS_004:
        return 429; // TOO_MANY_REQUESTS

      default:
        if (isSystemError()) {
          return 500; // INTERNAL_SERVER_ERROR
        }
        return 400; // BAD_REQUEST
    }
  }
}
