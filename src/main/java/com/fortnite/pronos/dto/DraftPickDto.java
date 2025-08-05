package com.fortnite.pronos.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fortnite.pronos.model.DraftPick;
import com.fortnite.pronos.model.Player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO pour la représentation d'un pick de draft */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftPickDto {

  private UUID id;
  private UUID draftId;
  private UUID playerId;
  private String playerName;
  private Player.Region playerRegion;
  private UUID pickerId;
  private String pickerName;
  private Integer round;
  private Integer pick;
  private Integer overallPick;
  private LocalDateTime pickTime;
  private Long timeTaken; // en secondes

  /** Créer un DraftPickDto à partir d'une entité DraftPick */
  public static DraftPickDto fromDraftPick(DraftPick draftPick) {
    DraftPickDto dto =
        DraftPickDto.builder()
            .id(draftPick.getId())
            .draftId(draftPick.getDraft().getId())
            .playerId(draftPick.getPlayer().getId())
            .playerName(draftPick.getPlayer().getNickname())
            .playerRegion(draftPick.getPlayer().getRegion())
            .pickerId(draftPick.getParticipant().getUser().getId())
            .pickerName(draftPick.getParticipant().getUser().getUsername())
            .round(draftPick.getRound())
            .pick(draftPick.getPickNumber())
            .pickTime(draftPick.getSelectionTime())
            .build();

    // Calculer l'overallPick
    int participantCount = draftPick.getDraft().getGame().getParticipants().size();
    dto.setOverallPick(((draftPick.getRound() - 1) * participantCount) + draftPick.getPickNumber());

    return dto;
  }

  /** Calculer le pick global basé sur le round et le pick */
  public int calculateOverallPick(int participantCount) {
    if (round == null || pick == null) {
      return 0;
    }
    return ((round - 1) * participantCount) + pick;
  }

  /** Vérifier si c'est un pick rapide (< 30 secondes) */
  public boolean isQuickPick() {
    return timeTaken != null && timeTaken < 30;
  }

  /** Vérifier si c'est un pick tardif (> 2 minutes) */
  public boolean isLatePick() {
    return timeTaken != null && timeTaken > 120;
  }
}
