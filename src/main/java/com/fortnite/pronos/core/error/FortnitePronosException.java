package com.fortnite.pronos.core.error;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Exception de base pour l'application Fortnite Pronos. Fournit une traÃ§abilitÃ© complÃ¨te selon
 * les standards aerospace.
 *
 * <p>Chaque exception possÃ¨de : - Un ID unique pour la traÃ§abilitÃ© - Un code d'erreur
 * standardisÃ© - Un timestamp prÃ©cis - Un contexte mÃ©tier - Des dÃ©tails techniques optionnels
 *
 * @author Fortnite Pronos Team
 * @version 1.0
 * @since 2025-06-09
 */
public class FortnitePronosException extends RuntimeException {

  private final UUID exceptionId;
  private final ErrorCode errorCode;
  private final OffsetDateTime timestamp;
  private final transient Map<String, Object> context;
  private final String userMessage;
  private final String technicalDetails;

  /**
   * Constructeur complet pour crÃ©er une exception avec tous les dÃ©tails.
   *
   * @param errorCode le code d'erreur standardisÃ©
   * @param userMessage message pour l'utilisateur final
   * @param technicalDetails dÃ©tails techniques pour le debug
   * @param context contexte mÃ©tier de l'erreur
   * @param cause exception racine (optionnel)
   */
  public FortnitePronosException(
      ErrorCode errorCode,
      String userMessage,
      String technicalDetails,
      Map<String, Object> context,
      Throwable cause) {
    super(buildMessage(errorCode, userMessage, technicalDetails), cause);
    this.exceptionId = UUID.randomUUID();
    this.errorCode = errorCode;
    this.timestamp = OffsetDateTime.now();
    this.userMessage = userMessage != null ? userMessage : errorCode.getDefaultMessage();
    this.technicalDetails = technicalDetails;
    this.context = context != null ? new HashMap<>(context) : new HashMap<>();
  }

  /**
   * Constructeur simplifiÃ© pour erreurs mÃ©tier courantes.
   *
   * @param errorCode le code d'erreur standardisÃ©
   * @param userMessage message pour l'utilisateur final
   */
  public FortnitePronosException(ErrorCode errorCode, String userMessage) {
    this(errorCode, userMessage, null, null, null);
  }

  /**
   * Constructeur pour erreurs avec cause.
   *
   * @param errorCode le code d'erreur standardisÃ©
   * @param userMessage message pour l'utilisateur final
   * @param cause exception racine
   */
  public FortnitePronosException(ErrorCode errorCode, String userMessage, Throwable cause) {
    this(errorCode, userMessage, null, null, cause);
  }

  /**
   * Constructeur avec contexte mÃ©tier.
   *
   * @param errorCode le code d'erreur standardisÃ©
   * @param context contexte mÃ©tier de l'erreur
   */
  public FortnitePronosException(ErrorCode errorCode, Map<String, Object> context) {
    this(errorCode, null, null, context, null);
  }

  /**
   * Constructeur minimal avec juste le code d'erreur.
   *
   * @param errorCode le code d'erreur standardisÃ©
   */
  public FortnitePronosException(ErrorCode errorCode) {
    this(errorCode, null, null, null, null);
  }

  private static String buildMessage(
      ErrorCode errorCode, String userMessage, String technicalDetails) {
    StringBuilder sb = new StringBuilder();
    sb.append("[").append(errorCode.getCode()).append("] ");

    if (userMessage != null) {
      sb.append(userMessage);
    } else {
      sb.append(errorCode.getDefaultMessage());
    }

    if (technicalDetails != null) {
      sb.append(" | DÃ©tails techniques: ").append(technicalDetails);
    }

    return sb.toString();
  }

  /**
   * Ajoute un Ã©lÃ©ment au contexte de l'erreur.
   *
   * @param key clÃ© du contexte
   * @param value valeur du contexte
   * @return cette exception pour chaÃ®nage
   */
  public FortnitePronosException addContext(String key, Object value) {
    this.context.put(key, value);
    return this;
  }

  /**
   * Ajoute plusieurs Ã©lÃ©ments au contexte.
   *
   * @param additionalContext contexte supplÃ©mentaire
   * @return cette exception pour chaÃ®nage
   */
  public FortnitePronosException addContext(Map<String, Object> additionalContext) {
    this.context.putAll(additionalContext);
    return this;
  }

  // === FACTORY METHODS POUR LES ERREURS COURANTES ===

  /**
   * CrÃ©e une exception de validation.
   *
   * @param errorCode code d'erreur de validation
   * @param fieldName nom du champ en erreur
   * @param fieldValue valeur du champ en erreur
   * @param userMessage message pour l'utilisateur
   * @return nouvelle exception de validation
   */
  public static FortnitePronosException validation(
      ErrorCode errorCode, String fieldName, Object fieldValue, String userMessage) {
    Map<String, Object> context = new HashMap<>();
    context.put("fieldName", fieldName);
    context.put("fieldValue", fieldValue);
    context.put("validationType", "field_validation");

    return new FortnitePronosException(errorCode, userMessage, null, context, null);
  }

  /**
   * CrÃ©e une exception mÃ©tier.
   *
   * @param errorCode code d'erreur mÃ©tier
   * @param entityType type d'entitÃ© concernÃ©e
   * @param entityId ID de l'entitÃ© concernÃ©e
   * @param userMessage message pour l'utilisateur
   * @return nouvelle exception mÃ©tier
   */
  public static FortnitePronosException business(
      ErrorCode errorCode, String entityType, Object entityId, String userMessage) {
    Map<String, Object> context = new HashMap<>();
    context.put("entityType", entityType);
    context.put("entityId", entityId);
    context.put("businessRule", errorCode.getCode());

    return new FortnitePronosException(errorCode, userMessage, null, context, null);
  }

  /**
   * CrÃ©e une exception systÃ¨me.
   *
   * @param errorCode code d'erreur systÃ¨me
   * @param technicalDetails dÃ©tails techniques
   * @param cause exception racine
   * @return nouvelle exception systÃ¨me
   */
  public static FortnitePronosException system(
      ErrorCode errorCode, String technicalDetails, Throwable cause) {
    Map<String, Object> context = new HashMap<>();
    context.put("systemComponent", "backend");
    context.put("severity", "high");

    return new FortnitePronosException(
        errorCode, errorCode.getDefaultMessage(), technicalDetails, context, cause);
  }

  // === GETTERS ===

  public UUID getExceptionId() {
    return exceptionId;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  public OffsetDateTime getTimestamp() {
    return timestamp;
  }

  public Map<String, Object> getContext() {
    return new HashMap<>(context);
  }

  public String getUserMessage() {
    return userMessage;
  }

  public String getTechnicalDetails() {
    return technicalDetails;
  }

  /**
   * GÃ©nÃ¨re un rapport complet de l'erreur pour les logs.
   *
   * @return rapport structurÃ© de l'erreur
   */
  public String generateErrorReport() {
    StringBuilder report = new StringBuilder();
    report.append("=== FORTNITE PRONOS ERROR REPORT ===\n");
    report.append("Exception ID: ").append(exceptionId).append("\n");
    report.append("Timestamp: ").append(timestamp).append("\n");
    report.append("Error Code: ").append(errorCode.getCode()).append("\n");
    report.append("Error Domain: ").append(errorCode.getDomain()).append("\n");
    report.append("Error Type: ").append(errorCode.getType()).append("\n");
    report.append("User Message: ").append(userMessage).append("\n");

    if (technicalDetails != null) {
      report.append("Technical Details: ").append(technicalDetails).append("\n");
    }

    if (!context.isEmpty()) {
      report.append("Context:\n");
      context.forEach(
          (key, value) -> report.append("  ").append(key).append(": ").append(value).append("\n"));
    }

    if (getCause() != null) {
      report
          .append("Root Cause: ")
          .append(getCause().getClass().getSimpleName())
          .append(" - ")
          .append(getCause().getMessage())
          .append("\n");
    }

    report.append("=====================================");
    return report.toString();
  }
}
