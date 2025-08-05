package com.fortnite.pronos.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FortniteTrackerPlayerStats {

  private String epicUserHandle;
  private String platformName;
  private String accountId;

  // Statistiques globales
  private Map<String, String> lifeTimeStats;

  // Statistiques compétitives
  private CompetitiveStats competitiveStats;

  // Statistiques récentes
  private RecentMatches recentMatches;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CompetitiveStats {
    private Integer score;
    private Integer scorePerMin;
    private Integer scorePerMatch;
    private Integer wins;
    private Integer top3;
    private Integer top5;
    private Integer top6;
    private Integer top10;
    private Integer top12;
    private Integer top25;
    private Integer kills;
    private Double kd;
    private Double winRatio;
    private Integer matches;
    private Double killsPerMin;
    private Double killsPerMatch;
    private Integer avgTimePlayed;
    private Integer scorePerKill;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecentMatches {
    private Match[] matches;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Match {
      private Long id;
      private String accountId;
      private String playlist;
      private Integer kills;
      private Integer minutesPlayed;
      private Integer top1;
      private Integer top3;
      private Integer top5;
      private Integer top6;
      private Integer top10;
      private Integer top12;
      private Integer top25;
      private Integer matches;
      private Integer score;
      private String platform;
      private String dateCollected;
    }
  }

  /** Récupère le score total du joueur */
  public Integer getTotalScore() {
    if (lifeTimeStats != null && lifeTimeStats.containsKey("Score")) {
      try {
        return Integer.parseInt(lifeTimeStats.get("Score").replace(",", ""));
      } catch (NumberFormatException e) {
        return 0;
      }
    }
    return 0;
  }

  /** Récupère le nombre total de victoires */
  public Integer getTotalWins() {
    if (lifeTimeStats != null && lifeTimeStats.containsKey("Wins")) {
      try {
        return Integer.parseInt(lifeTimeStats.get("Wins").replace(",", ""));
      } catch (NumberFormatException e) {
        return 0;
      }
    }
    return 0;
  }

  /** Récupère le K/D ratio */
  public Double getKDRatio() {
    if (lifeTimeStats != null && lifeTimeStats.containsKey("K/d")) {
      try {
        return Double.parseDouble(lifeTimeStats.get("K/d"));
      } catch (NumberFormatException e) {
        return 0.0;
      }
    }
    return 0.0;
  }

  /** Récupère le pourcentage de victoires */
  public Double getWinPercentage() {
    if (lifeTimeStats != null && lifeTimeStats.containsKey("Win%")) {
      try {
        return Double.parseDouble(lifeTimeStats.get("Win%").replace("%", ""));
      } catch (NumberFormatException e) {
        return 0.0;
      }
    }
    return 0.0;
  }
}
