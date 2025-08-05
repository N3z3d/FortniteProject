package com.fortnite.pronos.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fortnite.pronos.model.Draft;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO pour la représentation d'un draft */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftDto {

  private UUID id;
  private UUID gameId;
  private String gameName;
  private Draft.Status status;
  private Integer currentRound;
  private Integer currentPick;
  private Integer totalRounds;
  private LocalDateTime startedAt;
  private LocalDateTime finishedAt;
  private List<UUID> participantOrder;
  private UUID currentPickerId;
  private String currentPickerName;
  private Integer timeRemaining;

  /** Créer un DraftDto à partir d'une entité Draft */
  public static DraftDto fromDraft(Draft draft) {
    return DraftDto.builder()
        .id(draft.getId())
        .gameId(draft.getGame().getId())
        .gameName(draft.getGame().getName())
        .status(draft.getStatus())
        .currentRound(draft.getCurrentRound())
        .currentPick(draft.getCurrentPick())
        .totalRounds(draft.getTotalRounds())
        .startedAt(draft.getStartedAt())
        .finishedAt(draft.getFinishedAt())
        .build();
  }

  /** Vérifier si le draft est actif */
  public boolean isActive() {
    return status == Draft.Status.ACTIVE;
  }

  /** Vérifier si le draft est terminé */
  public boolean isFinished() {
    return status == Draft.Status.FINISHED;
  }

  /** Calculer le pourcentage de progression */
  public double getProgressPercentage() {
    if (totalRounds == null || totalRounds == 0) {
      return 0.0;
    }

    int totalPicks = totalRounds * (participantOrder != null ? participantOrder.size() : 1);
    int currentProgress =
        ((currentRound - 1) * (participantOrder != null ? participantOrder.size() : 1))
            + currentPick;

    return (double) currentProgress / totalPicks * 100.0;
  }

  /** Obtenir le nombre de picks restants */
  public int getRemainingPicks() {
    if (totalRounds == null || participantOrder == null) {
      return 0;
    }

    int totalPicks = totalRounds * participantOrder.size();
    int currentProgress = ((currentRound - 1) * participantOrder.size()) + currentPick;

    return totalPicks - currentProgress;
  }
}
