package com.fortnite.pronos.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fortnite.pronos.model.Draft;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO pour le statut d'un draft */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DraftStatus {

  private UUID draftId;
  private UUID gameId;
  private Draft.Status status;
  private Integer currentRound;
  private Integer currentPick;
  private Integer totalRounds;
  private Integer totalParticipants;
  private String currentParticipantUsername;
  private UUID currentParticipantId;
  private LocalDateTime lastPickTime;
  private LocalDateTime nextPickDeadline;
  private Boolean isCurrentUserTurn;
  private Integer timeRemainingSeconds;
  private Boolean autoPickEnabled;
  private Integer autoPickDelaySeconds;

  /** Créer un DraftStatus à partir d'un Draft */
  public static DraftStatus fromDraft(
      Draft draft,
      String currentParticipantUsername,
      UUID currentParticipantId,
      Boolean isCurrentUserTurn) {
    return DraftStatus.builder()
        .draftId(draft.getId())
        .gameId(draft.getGame().getId())
        .status(draft.getStatus())
        .currentRound(draft.getCurrentRound())
        .currentPick(draft.getCurrentPick())
        .totalRounds(draft.getTotalRounds())
        .totalParticipants(draft.getGame().getParticipants().size())
        .currentParticipantUsername(currentParticipantUsername)
        .currentParticipantId(currentParticipantId)
        .lastPickTime(draft.getUpdatedAt())
        .nextPickDeadline(calculateNextPickDeadline(draft))
        .isCurrentUserTurn(isCurrentUserTurn)
        .timeRemainingSeconds(calculateTimeRemaining(draft))
        .autoPickEnabled(true) // Par défaut activé
        .autoPickDelaySeconds(43200) // 12 heures par défaut
        .build();
  }

  /** Calculer la deadline du prochain pick */
  private static LocalDateTime calculateNextPickDeadline(Draft draft) {
    if (draft.getUpdatedAt() == null) {
      return LocalDateTime.now().plusHours(12);
    }
    return draft.getUpdatedAt().plusHours(12);
  }

  /** Calculer le temps restant en secondes */
  private static Integer calculateTimeRemaining(Draft draft) {
    if (draft.getUpdatedAt() == null) {
      return 43200; // 12 heures par défaut
    }

    LocalDateTime deadline = draft.getUpdatedAt().plusHours(12);
    LocalDateTime now = LocalDateTime.now();

    if (now.isAfter(deadline)) {
      return 0;
    }

    return (int) java.time.Duration.between(now, deadline).getSeconds();
  }

  /** Vérifier si le draft est actif */
  public boolean isActive() {
    return status == Draft.Status.ACTIVE;
  }

  /** Vérifier si le draft est en pause */
  public boolean isPaused() {
    return status == Draft.Status.PAUSED;
  }

  /** Vérifier si le draft est terminé */
  public boolean isFinished() {
    return status == Draft.Status.FINISHED;
  }

  /** Vérifier si le draft est annulé */
  public boolean isCancelled() {
    return status == Draft.Status.CANCELLED;
  }

  /** Vérifier si c'est le tour de l'utilisateur actuel */
  public boolean isUserTurn() {
    return Boolean.TRUE.equals(isCurrentUserTurn);
  }

  /** Vérifier si le temps est écoulé */
  public boolean isTimeExpired() {
    return timeRemainingSeconds != null && timeRemainingSeconds <= 0;
  }

  /** Obtenir le temps restant formaté */
  public String getTimeRemainingFormatted() {
    if (timeRemainingSeconds == null || timeRemainingSeconds <= 0) {
      return "Temps écoulé";
    }

    int hours = timeRemainingSeconds / 3600;
    int minutes = (timeRemainingSeconds % 3600) / 60;
    int seconds = timeRemainingSeconds % 60;

    if (hours > 0) {
      return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    } else {
      return String.format("%02d:%02d", minutes, seconds);
    }
  }

  /** Obtenir le statut lisible */
  public String getStatusLabel() {
    switch (status) {
      case PENDING:
        return "En attente";
      case ACTIVE:
        return "En cours";
      case PAUSED:
        return "En pause";
      case FINISHED:
        return "Terminé";
      case CANCELLED:
        return "Annulé";
      default:
        return "Inconnu";
    }
  }

  /** Obtenir la progression du draft en pourcentage */
  public int getProgressPercentage() {
    if (totalRounds == null || totalRounds == 0) {
      return 0;
    }

    int totalPicks = totalRounds * totalParticipants;

    // Éviter division par zéro quand il n'y a pas de participants
    if (totalPicks == 0) {
      return 0;
    }

    int currentPickNumber = (currentRound - 1) * totalParticipants + currentPick;
    return Math.min(100, (currentPickNumber * 100) / totalPicks);
  }

  /** Obtenir le numéro de pick actuel formaté */
  public String getCurrentPickFormatted() {
    return String.format("R%dP%d", currentRound, currentPick);
  }

  /** Obtenir le nombre de picks restants */
  public int getRemainingPicks() {
    if (totalRounds == null || totalParticipants == null) {
      return 0;
    }

    int totalPicks = totalRounds * totalParticipants;
    int currentPickNumber = (currentRound - 1) * totalParticipants + currentPick;

    return Math.max(0, totalPicks - currentPickNumber);
  }
}
