package com.fortnite.pronos.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO pour représenter les détails complets d'une game */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameDetailDto {

  private UUID gameId;
  private String gameName;
  private String description;
  private String creatorName;
  private String status;
  private String invitationCode;
  private Integer maxParticipants;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private List<ParticipantInfo> participants;
  private DraftInfo draftInfo;
  private GameStatistics statistics;

  /** Information sur un participant */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ParticipantInfo {
    private UUID participantId;
    private String username;
    private String email;
    private LocalDateTime joinedAt;
    private Integer joinOrder;
    private Boolean isCreator;
    private Integer totalPlayers;
    private List<PlayerInfo> selectedPlayers;
  }

  /** Information sur un joueur sélectionné */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PlayerInfo {
    private UUID playerId;
    private String nickname;
    private String region;
    private String tranche;
    private Integer currentScore;
  }

  /** Information sur le draft */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DraftInfo {
    private UUID draftId;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime pausedAt;
    private Integer currentRound;
    private Integer currentPick;
    private String currentPickerUsername;
  }

  /** Statistiques de la game */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GameStatistics {
    private Integer totalParticipants;
    private Integer totalPlayers;
    private Map<String, Integer> regionDistribution;
    private Double averagePlayersPerParticipant;
    private Integer remainingSlots;
  }
}
