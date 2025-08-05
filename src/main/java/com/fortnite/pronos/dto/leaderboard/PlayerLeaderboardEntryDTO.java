package com.fortnite.pronos.dto.leaderboard;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO pour les entrées du classement des joueurs */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerLeaderboardEntryDTO {

  private UUID playerId;
  private String nickname;
  private String username;
  private String region;
  private String tranche;
  private String country;
  private Integer rank;
  private Integer totalPoints;
  private Integer totalKills;
  private Integer gamesPlayed;
  private Double avgPointsPerGame;
  private Double avgKillsPerGame;
  private Integer bestScore;
  private String bestTournament;
  private Integer teamsCount;

  /** Calcule la moyenne de points par partie */
  public Double getAvgPointsPerGame() {
    if (gamesPlayed == null || gamesPlayed == 0) {
      return 0.0;
    }
    return totalPoints != null ? (double) totalPoints / gamesPlayed : 0.0;
  }

  /** Calcule la moyenne de kills par partie */
  public Double getAvgKillsPerGame() {
    if (gamesPlayed == null || gamesPlayed == 0) {
      return 0.0;
    }
    return totalKills != null ? (double) totalKills / gamesPlayed : 0.0;
  }

  /** Détermine si le joueur est un top performer */
  public boolean isTopPerformer() {
    return rank != null && rank <= 10;
  }

  /** Retourne le niveau de performance */
  public String getPerformanceLevel() {
    if (rank == null) return "UNRANKED";
    if (rank <= 5) return "ELITE";
    if (rank <= 15) return "EXCELLENT";
    if (rank <= 50) return "GOOD";
    return "AVERAGE";
  }
}
