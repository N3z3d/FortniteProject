package com.fortnite.pronos.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fortnite.pronos.model.GameStatus;
import com.fortnite.pronos.model.Player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO pour la représentation des games */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameDto {

  private UUID id;
  private String name;
  private UUID creatorId;
  private String creatorUsername;
  private Integer maxParticipants;
  private Integer currentParticipantCount;
  private GameStatus status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Integer currentSeason;
  private String description;
  private String invitationCode;
  private Boolean isPrivate;
  private Boolean autoStartDraft;
  private Integer draftTimeLimit;
  private Integer autoPickDelay;
  private Map<Player.Region, Integer> regionRules;
  private Map<UUID, String> participants;
  private Map<String, Object> statistics;

  /** Calculer le nombre de places disponibles */
  public int getAvailableSlots() {
    if (maxParticipants == null || currentParticipantCount == null) {
      return 0;
    }
    return Math.max(0, maxParticipants - currentParticipantCount);
  }

  /** Vérifier si la game est pleine */
  public boolean isFull() {
    return getAvailableSlots() == 0;
  }

  /** Vérifier si la game est disponible pour rejoindre */
  public boolean isAvailableToJoin() {
    return status == GameStatus.CREATING && !isFull();
  }

  /** Vérifier si la game est en cours de draft */
  public boolean isInDraft() {
    return status == GameStatus.DRAFTING;
  }

  /** Vérifier si la game est active */
  public boolean isActive() {
    return status == GameStatus.ACTIVE;
  }

  /** Vérifier si la game est terminée */
  public boolean isFinished() {
    return status == GameStatus.FINISHED;
  }

  /** Vérifier si la game est annulée */
  public boolean isCancelled() {
    return status == GameStatus.CANCELLED;
  }

  /** Calculer le pourcentage de remplissage */
  public double getFillPercentage() {
    if (maxParticipants == null || maxParticipants == 0 || currentParticipantCount == null) {
      return 0.0;
    }
    return (double) currentParticipantCount / maxParticipants * 100.0;
  }

  /** Initialiser les collections si elles sont nulles */
  public void initializeCollections() {
    if (regionRules == null) {
      regionRules = new HashMap<>();
    }
    if (participants == null) {
      participants = new HashMap<>();
    }
    if (statistics == null) {
      statistics = new HashMap<>();
    }
  }

  /** Ajouter une règle de région */
  public void addRegionRule(Player.Region region, Integer maxPlayers) {
    initializeCollections();
    regionRules.put(region, maxPlayers);
  }

  /** Ajouter un participant */
  public void addParticipant(UUID participantId, String participantName) {
    initializeCollections();
    participants.put(participantId, participantName);
  }

  /** Ajouter une statistique */
  public void addStatistic(String key, Object value) {
    initializeCollections();
    statistics.put(key, value);
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

  /** Obtenir le nombre total de joueurs autorisés par les règles de région */
  public int getTotalAllowedPlayers() {
    if (regionRules == null) {
      return 0;
    }
    return regionRules.values().stream().mapToInt(Integer::intValue).sum();
  }

  /** Vérifier si la game peut démarrer le draft */
  public boolean canStartDraft() {
    return status == GameStatus.CREATING
        && currentParticipantCount != null
        && currentParticipantCount >= 2;
  }

  /** Vérifier si la game peut être rejointe par code d'invitation */
  public boolean canJoinByInvitationCode() {
    return isPrivate != null && isPrivate && invitationCode != null && !invitationCode.isEmpty();
  }

  /** Obtenir le statut sous forme de string */
  public String getStatusString() {
    return status != null ? status.name() : null;
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

  /** Obtenir le nom court de la game (max 50 caractères) */
  public String getShortName() {
    if (name == null) {
      return "";
    }
    return name.length() > 50 ? name.substring(0, 47) + "..." : name;
  }

  /** Obtenir la description courte (max 100 caractères) */
  public String getShortDescription() {
    if (description == null) {
      return "";
    }
    return description.length() > 100 ? description.substring(0, 97) + "..." : description;
  }

  /** Vérifier si la game est récente (créée dans les 24h) */
  public boolean isRecent() {
    if (createdAt == null) {
      return false;
    }
    LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
    return createdAt.isAfter(twentyFourHoursAgo);
  }

  /** Vérifier si la game a été mise à jour récemment (dans les 1h) */
  public boolean wasRecentlyUpdated() {
    if (updatedAt == null) {
      return false;
    }
    LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
    return updatedAt.isAfter(oneHourAgo);
  }

  /** Obtenir le temps écoulé depuis la création */
  public long getTimeSinceCreation() {
    if (createdAt == null) {
      return 0;
    }
    return java.time.Duration.between(createdAt, LocalDateTime.now()).toMinutes();
  }

  /** Obtenir le temps écoulé depuis la dernière mise à jour */
  public long getTimeSinceLastUpdate() {
    if (updatedAt == null) {
      return 0;
    }
    return java.time.Duration.between(updatedAt, LocalDateTime.now()).toMinutes();
  }

  /**
   * Get participant count - alias for currentParticipantCount for accessibility service
   * compatibility This method provides WCAG-compliant access to participant information
   */
  public Integer getParticipantCount() {
    return this.currentParticipantCount;
  }

  /** Créer un GameDto à partir d'une entité Game */
  public static GameDto fromGame(com.fortnite.pronos.model.Game game) {
    if (game == null) {
      throw new IllegalArgumentException("Game cannot be null");
    }

    GameDto dto = new GameDto();
    dto.setId(game.getId());
    dto.setName(game.getName());
    dto.setDescription(game.getDescription());

    // Gestion robuste du creator (peut être null ou lazy-loaded)
    if (game.getCreator() != null) {
      dto.setCreatorId(game.getCreator().getId());
      dto.setCreatorUsername(game.getCreator().getUsername());
    }

    dto.setMaxParticipants(game.getMaxParticipants());
    dto.setCurrentParticipantCount(
        game.getParticipants() != null ? game.getParticipants().size() : 0);
    dto.setStatus(game.getStatus());
    dto.setCreatedAt(game.getCreatedAt());
    dto.setInvitationCode(game.getInvitationCode());

    // Initialiser les collections
    dto.initializeCollections();

    // Ajouter les règles régionales
    if (game.getRegionRules() != null) {
      for (var rule : game.getRegionRules()) {
        if (rule != null) {
          dto.addRegionRule(rule.getRegion(), rule.getMaxPlayers());
        }
      }
    }

    // Ajouter les participants avec gestion robuste des relations
    if (game.getParticipants() != null) {
      for (var participant : game.getParticipants()) {
        if (participant != null && participant.getUser() != null) {
          dto.addParticipant(participant.getUser().getId(), participant.getUser().getUsername());
        }
      }
    }

    return dto;
  }
}
