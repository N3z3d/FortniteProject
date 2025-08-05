package com.fortnite.pronos.dto;

import java.util.List;
import java.util.UUID;

import com.fortnite.pronos.model.Player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryDTO {

  // Informations de l'équipe
  private UUID teamId;
  private String teamName;
  private Integer rank;
  private Long totalPoints;

  // Informations du propriétaire
  private UUID ownerId;
  private String ownerUsername;

  // Joueurs de l'équipe
  private List<PlayerInfo> players;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PlayerInfo {
    private UUID playerId;
    private String username;
    private String nickname;
    private Player.Region region;
    private String tranche;
    private Long points;
  }
}
