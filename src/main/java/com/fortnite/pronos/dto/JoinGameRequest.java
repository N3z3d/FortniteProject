package com.fortnite.pronos.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO pour rejoindre une game */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JoinGameRequest {

  private UUID gameId;
  private UUID userId;
  private String invitationCode;
  private Boolean joinAsSpectator;

  // Constructeur avec paramètres essentiels
  public JoinGameRequest(UUID gameId, UUID userId) {
    this.gameId = gameId;
    this.userId = userId;
    this.invitationCode = null;
    this.joinAsSpectator = false;
  }

  // Constructeur avec tous les paramètres
  public JoinGameRequest(UUID gameId, UUID userId, String invitationCode, Boolean joinAsSpectator) {
    this.gameId = gameId;
    this.userId = userId;
    this.invitationCode = invitationCode;
    this.joinAsSpectator = joinAsSpectator;
  }

  // Constantes de validation
  private static final int MIN_INVITATION_CODE_LENGTH = 3;
  private static final int MAX_INVITATION_CODE_LENGTH = 20;
  private static final String INVITATION_CODE_PATTERN = "^[A-Za-z0-9-]+$";

  private List<String> validationErrors = new ArrayList<>();

  /** Valider la requête de rejoindre une game */
  public boolean isValid() {
    validationErrors.clear();

    validateGameId();
    validateUserId();
    validateInvitationCode();

    return validationErrors.isEmpty();
  }

  /** Valider l'ID de la game */
  private void validateGameId() {
    if (gameId == null) {
      validationErrors.add("L'ID de la game est requis");
    }
  }

  /** Valider l'ID de l'utilisateur */
  private void validateUserId() {
    if (userId == null) {
      validationErrors.add("L'ID de l'utilisateur est requis");
    }
  }

  /** Valider le code d'invitation */
  private void validateInvitationCode() {
    if (invitationCode == null || invitationCode.trim().isEmpty()) {
      return; // Le code d'invitation est optionnel
    }

    String trimmedCode = invitationCode.trim();

    if (trimmedCode.length() < MIN_INVITATION_CODE_LENGTH
        || trimmedCode.length() > MAX_INVITATION_CODE_LENGTH) {
      validationErrors.add(
          "Le code d'invitation doit contenir entre "
              + MIN_INVITATION_CODE_LENGTH
              + " et "
              + MAX_INVITATION_CODE_LENGTH
              + " caractères");
      return;
    }

    if (!trimmedCode.matches(INVITATION_CODE_PATTERN)) {
      validationErrors.add(
          "Le code d'invitation ne peut contenir que des lettres, chiffres et tirets");
    }
  }

  /** Vérifier si la requête a un code d'invitation */
  public boolean hasInvitationCode() {
    return invitationCode != null && !invitationCode.trim().isEmpty();
  }

  /** Vérifier si la requête est pour rejoindre comme spectateur */
  public boolean isSpectator() {
    return joinAsSpectator != null && joinAsSpectator;
  }

  /** Vérifier si la requête est pour rejoindre comme participant */
  public boolean isParticipant() {
    return !isSpectator();
  }

  /** Obtenir le code d'invitation en majuscules */
  public String getInvitationCodeUpperCase() {
    if (invitationCode == null) {
      return null;
    }
    return invitationCode.toUpperCase();
  }

  /** Obtenir le type de participation */
  public String getParticipationType() {
    return isSpectator() ? "SPECTATOR" : "PARTICIPANT";
  }

  /** Ajouter une erreur de validation */
  public void addValidationError(String error) {
    if (validationErrors == null) {
      validationErrors = new ArrayList<>();
    }
    validationErrors.add(error);
  }

  /** Effacer les erreurs de validation */
  public void clearValidationErrors() {
    validationErrors.clear();
  }

  /** Obtenir les erreurs de validation */
  public List<String> getValidationErrors() {
    if (validationErrors == null) {
      validationErrors = new ArrayList<>();
    }
    return validationErrors;
  }

  /** Obtenir le code d'invitation normalisé (trim et majuscules) */
  public String getNormalizedInvitationCode() {
    if (invitationCode == null) {
      return null;
    }
    return invitationCode.trim().toUpperCase();
  }

  /** Vérifier si le code d'invitation est valide */
  public boolean isInvitationCodeValid() {
    if (!hasInvitationCode()) {
      return true; // Pas de code = valide
    }

    String trimmedCode = invitationCode.trim();

    if (trimmedCode.length() < MIN_INVITATION_CODE_LENGTH
        || trimmedCode.length() > MAX_INVITATION_CODE_LENGTH) {
      return false;
    }

    return trimmedCode.matches(INVITATION_CODE_PATTERN);
  }

  /** Obtenir la description du type de participation */
  public String getParticipationTypeDescription() {
    return isSpectator() ? "Spectateur" : "Participant";
  }

  /** Vérifier si la requête nécessite un code d'invitation */
  public boolean requiresInvitationCode() {
    // Cette méthode pourrait être utilisée pour déterminer si un code est requis
    // selon les règles métier de la game
    return hasInvitationCode();
  }

  /** Obtenir un résumé de la requête */
  public String getRequestSummary() {
    StringBuilder summary = new StringBuilder();
    summary.append("Rejoindre la game ").append(gameId);
    summary.append(" en tant que ").append(getParticipationTypeDescription());

    if (hasInvitationCode()) {
      summary.append(" avec le code ").append(getNormalizedInvitationCode());
    }

    return summary.toString();
  }

  /** Vérifier si la requête est complète */
  public boolean isComplete() {
    return gameId != null && userId != null;
  }

  /** Obtenir les paramètres manquants */
  public List<String> getMissingParameters() {
    List<String> missing = new ArrayList<>();

    if (gameId == null) {
      missing.add("gameId");
    }

    if (userId == null) {
      missing.add("userId");
    }

    return missing;
  }

  /** Vérifier si la requête peut être traitée */
  public boolean canBeProcessed() {
    return isComplete() && isValid();
  }

  /** Obtenir le statut de validation */
  public String getValidationStatus() {
    if (isValid()) {
      return "VALID";
    } else {
      return "INVALID";
    }
  }

  /** Obtenir le nombre d'erreurs de validation */
  public int getValidationErrorCount() {
    return validationErrors != null ? validationErrors.size() : 0;
  }

  /** Vérifier si la requête a des erreurs de validation */
  public boolean hasValidationErrors() {
    return getValidationErrorCount() > 0;
  }

  /** Obtenir la première erreur de validation */
  public String getFirstValidationError() {
    if (validationErrors != null && !validationErrors.isEmpty()) {
      return validationErrors.get(0);
    }
    return null;
  }

  /** Obtenir toutes les erreurs de validation comme une seule chaîne */
  public String getAllValidationErrorsAsString() {
    if (validationErrors == null || validationErrors.isEmpty()) {
      return "";
    }
    return String.join("; ", validationErrors);
  }
}
