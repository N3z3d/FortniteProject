package com.fortnite.pronos.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO pour représenter le score d'une équipe sur une période */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamScoreDto {

  private UUID teamId;
  private String teamName;
  private int totalScore;
  private LocalDate startDate;
  private LocalDate endDate;
  private List<PlayerScore> playerScores;

  /** Score détaillé par joueur */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PlayerScore {
    private UUID playerId;
    private String playerName;
    private String playerRegion;
    private int totalPoints;
    private int scoreCount;
    private double averageScore;
    private int minScore;
    private int maxScore;
    private double medianScore;

    /** Calculer les statistiques à partir d'une liste de scores */
    public static PlayerScore fromScores(
        UUID playerId, String playerName, String playerRegion, List<Integer> scores) {

      if (scores.isEmpty()) {
        return PlayerScore.builder()
            .playerId(playerId)
            .playerName(playerName)
            .playerRegion(playerRegion)
            .totalPoints(0)
            .scoreCount(0)
            .averageScore(0.0)
            .minScore(0)
            .maxScore(0)
            .medianScore(0.0)
            .build();
      }

      int total = scores.stream().mapToInt(Integer::intValue).sum();
      double average = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
      int min = scores.stream().mapToInt(Integer::intValue).min().orElse(0);
      int max = scores.stream().mapToInt(Integer::intValue).max().orElse(0);

      // Calcul de la médiane
      List<Integer> sortedScores = scores.stream().sorted().toList();
      double median =
          sortedScores.size() % 2 == 0
              ? (sortedScores.get(sortedScores.size() / 2 - 1)
                      + sortedScores.get(sortedScores.size() / 2))
                  / 2.0
              : sortedScores.get(sortedScores.size() / 2);

      return PlayerScore.builder()
          .playerId(playerId)
          .playerName(playerName)
          .playerRegion(playerRegion)
          .totalPoints(total)
          .scoreCount(scores.size())
          .averageScore(average)
          .minScore(min)
          .maxScore(max)
          .medianScore(median)
          .build();
    }
  }
}
