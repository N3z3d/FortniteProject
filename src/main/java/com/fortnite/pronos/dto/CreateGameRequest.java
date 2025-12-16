package com.fortnite.pronos.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.fortnite.pronos.model.Player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO pour la création d'une game */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateGameRequest {

  @NotBlank(message = "Le nom de la game est requis")
  @Size(min = 3, max = 50, message = "Le nom doit contenir entre 3 et 50 caractères")
  private String name;

  @NotNull(message = "Le nombre maximum de participants est requis") @Min(value = 2, message = "Minimum 2 participants requis")
  @Max(value = 20, message = "Maximum 20 participants autorisés")
  private Integer maxParticipants;

  @Size(max = 500, message = "La description ne peut pas dépasser 500 caractères")
  private String description;

  private Boolean isPrivate;

  private Boolean autoStartDraft;

  @Min(value = 60, message = "Minimum 60 secondes pour la limite de temps")
  @Max(value = 3600, message = "Maximum 1 heure pour la limite de temps")
  private Integer draftTimeLimit;

  @Min(value = 300, message = "Minimum 5 minutes pour l'auto-pick")
  @Max(value = 86400, message = "Maximum 24 heures pour l'auto-pick")
  private Integer autoPickDelay;

  @Min(value = 2020, message = "Saison invalide")
  @Max(value = 2030, message = "Saison invalide")
  private Integer currentSeason;

  private UUID creatorId;

  @Valid
  private Map<
          Player.Region,
          @Positive(message = "Le nombre de joueurs par région doit être positif") Integer>
      regionRules;

  // Constructeur avec paramètres essentiels
  public CreateGameRequest(
      String name,
      Integer maxParticipants,
      String description,
      Boolean isPrivate,
      Boolean autoStartDraft,
      Integer draftTimeLimit,
      Integer autoPickDelay,
      Integer currentSeason) {
    this.name = name;
    this.maxParticipants = maxParticipants;
    this.description = description;
    this.isPrivate = isPrivate;
    this.autoStartDraft = autoStartDraft;
    this.draftTimeLimit = draftTimeLimit;
    this.autoPickDelay = autoPickDelay;
    this.currentSeason = currentSeason;
    this.regionRules = new HashMap<>();
  }

  // Constantes de validation
  private static final int MIN_NAME_LENGTH = 3;
  private static final int MAX_NAME_LENGTH = 50;
  private static final int MAX_DESCRIPTION_LENGTH = 500;
  private static final int MIN_MAX_PARTICIPANTS = 2;
  private static final int MAX_MAX_PARTICIPANTS = 20;
  private static final int MIN_DRAFT_TIME_LIMIT = 60; // 1 minute
  private static final int MAX_DRAFT_TIME_LIMIT = 3600; // 1 heure
  private static final int MIN_AUTO_PICK_DELAY = 300; // 5 minutes
  private static final int MAX_AUTO_PICK_DELAY = 86400; // 24 heures
  private static final int MAX_PLAYERS_PER_REGION = 10;

  private List<String> validationErrors = new ArrayList<>();

  /** Valider la requête de création de game */
  public boolean isValid() {
    validationErrors.clear();

    validateName();
    validateMaxParticipants();
    validateDescription();
    validateDraftTimeLimit();
    validateAutoPickDelay();
    validateRegionRules();
    validateRegionRulesConsistency();

    return validationErrors.isEmpty();
  }

  /** Valider le nom de la game */
  private void validateName() {
    if (name == null || name.trim().isEmpty()) {
      validationErrors.add("Le nom de la game ne peut pas être vide");
      return;
    }

    if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
      validationErrors.add(
          "Le nom de la game doit contenir entre "
              + MIN_NAME_LENGTH
              + " et "
              + MAX_NAME_LENGTH
              + " caractères");
    }
  }

  /** Valider le nombre maximum de participants */
  private void validateMaxParticipants() {
    if (maxParticipants == null) {
      validationErrors.add("Le nombre maximum de participants est requis");
      return;
    }

    if (maxParticipants < MIN_MAX_PARTICIPANTS || maxParticipants > MAX_MAX_PARTICIPANTS) {
      validationErrors.add(
          "Le nombre maximum de participants doit être entre "
              + MIN_MAX_PARTICIPANTS
              + " et "
              + MAX_MAX_PARTICIPANTS);
    }
  }

  /** Valider la description */
  private void validateDescription() {
    if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
      validationErrors.add(
          "La description ne peut pas dépasser " + MAX_DESCRIPTION_LENGTH + " caractères");
    }
  }

  /** Valider la limite de temps du draft */
  private void validateDraftTimeLimit() {
    if (draftTimeLimit != null) {
      if (draftTimeLimit < MIN_DRAFT_TIME_LIMIT || draftTimeLimit > MAX_DRAFT_TIME_LIMIT) {
        validationErrors.add(
            "La limite de temps par sélection doit être entre "
                + MIN_DRAFT_TIME_LIMIT
                + " et "
                + MAX_DRAFT_TIME_LIMIT
                + " secondes");
      }
    }
  }

  /** Valider le délai d'auto-pick */
  private void validateAutoPickDelay() {
    if (autoPickDelay != null) {
      if (autoPickDelay < MIN_AUTO_PICK_DELAY || autoPickDelay > MAX_AUTO_PICK_DELAY) {
        validationErrors.add(
            "Le délai d'auto-pick doit être entre "
                + MIN_AUTO_PICK_DELAY
                + " et "
                + MAX_AUTO_PICK_DELAY
                + " secondes");
      }
    }
  }

  /** Valider les règles de région */
  private void validateRegionRules() {
    if (regionRules == null) {
      return;
    }

    for (Map.Entry<Player.Region, Integer> entry : regionRules.entrySet()) {
      Player.Region region = entry.getKey();
      Integer maxPlayers = entry.getValue();

      if (maxPlayers == null || maxPlayers <= 0) {
        validationErrors.add("Le nombre de joueurs par région doit être positif");
        return;
      }

      if (maxPlayers > MAX_PLAYERS_PER_REGION) {
        validationErrors.add(
            "Le nombre de joueurs par région ne peut pas dépasser " + MAX_PLAYERS_PER_REGION);
        return;
      }
    }
  }

  /** Valider la cohérence entre les règles de région et maxParticipants */
  private void validateRegionRulesConsistency() {
    if (regionRules == null || maxParticipants == null) {
      return;
    }

    int totalPlayers = getTotalPlayersFromRegionRules();
    if (totalPlayers > maxParticipants) {
      validationErrors.add(
          "Le total des joueurs par région ("
              + totalPlayers
              + ") ne peut pas dépasser le nombre maximum de participants ("
              + maxParticipants
              + ")");
    }
  }

  /** Calculer le nombre total de joueurs des règles de région */
  public int getTotalPlayersFromRegionRules() {
    if (regionRules == null) {
      return 0;
    }
    return regionRules.values().stream().mapToInt(Integer::intValue).sum();
  }

  /** Vérifier si les règles de région sont cohérentes avec maxParticipants */
  public boolean areRegionRulesConsistent() {
    if (regionRules == null || maxParticipants == null) {
      return true;
    }
    return getTotalPlayersFromRegionRules() <= maxParticipants;
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

  /** Ajouter une règle de région */
  public void addRegionRule(Player.Region region, Integer maxPlayers) {
    if (regionRules == null) {
      regionRules = new HashMap<>();
    }
    regionRules.put(region, maxPlayers);
  }

  /** Obtenir le nombre de joueurs maximum pour une région */
  public Integer getMaxPlayersForRegion(Player.Region region) {
    if (regionRules == null) {
      return null;
    }
    return regionRules.get(region);
  }

  /** Vérifier si une région a une règle définie */
  public boolean hasRegionRule(Player.Region region) {
    return regionRules != null && regionRules.containsKey(region);
  }

  /** Obtenir la durée du draft en minutes */
  public int getDraftTimeLimitMinutes() {
    return draftTimeLimit != null ? draftTimeLimit / 60 : 0;
  }

  /** Obtenir le délai d'auto-pick en heures */
  public int getAutoPickDelayHours() {
    return autoPickDelay != null ? autoPickDelay / 3600 : 0;
  }

  /** Vérifier si l'auto-pick est activé */
  public boolean isAutoPickEnabled() {
    return autoPickDelay != null && autoPickDelay > 0;
  }

  /** Vérifier si le draft automatique est activé */
  public boolean isAutoDraftEnabled() {
    return autoStartDraft != null && autoStartDraft;
  }

  /** Obtenir le nom court de la game (max 30 caractères) */
  public String getShortName() {
    if (name == null) {
      return "";
    }
    return name.length() > 30 ? name.substring(0, 27) + "..." : name;
  }

  /** Obtenir la description courte (max 100 caractères) */
  public String getShortDescription() {
    if (description == null) {
      return "";
    }
    return description.length() > 100 ? description.substring(0, 97) + "..." : description;
  }

  /** Vérifier si la game est privée */
  public boolean isPrivateGame() {
    return isPrivate != null && isPrivate;
  }

  /** Obtenir la saison courante (défaut 2025) */
  public int getCurrentSeasonOrDefault() {
    return currentSeason != null ? currentSeason : 2025;
  }

  /** Obtenir le nombre maximum de participants (défaut 8) */
  public int getMaxParticipantsOrDefault() {
    return maxParticipants != null ? maxParticipants : 8;
  }

  /** Obtenir la limite de temps du draft (défaut 5 minutes) */
  public int getDraftTimeLimitOrDefault() {
    return draftTimeLimit != null ? draftTimeLimit : 300;
  }

  /** Obtenir le délai d'auto-pick (défaut 12 heures) */
  public int getAutoPickDelayOrDefault() {
    return autoPickDelay != null ? autoPickDelay : 43200;
  }
}
