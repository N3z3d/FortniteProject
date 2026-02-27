package com.fortnite.pronos.dto;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO representing a team score over a date range. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamScoreDto {

  private static final int ZERO_SCORE = 0;
  private static final int MEDIAN_DIVISOR = 2;
  private static final int INDEX_OFFSET = 1;
  private static final double EVEN_MEDIAN_DENOMINATOR = 2.0;

  private UUID teamId;
  private String teamName;
  private int totalScore;
  private LocalDate startDate;
  private LocalDate endDate;
  private List<PlayerScore> playerScores;

  /** Build per-player score statistics from raw score values. */
  public static PlayerScore fromScores(
      UUID playerId, String playerName, String playerRegion, Collection<Integer> scores) {
    if (scores.isEmpty()) {
      return emptyPlayerScore(playerId, playerName, playerRegion);
    }

    int total = scores.stream().mapToInt(Integer::intValue).sum();
    double average = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    int min = scores.stream().mapToInt(Integer::intValue).min().orElse(ZERO_SCORE);
    int max = scores.stream().mapToInt(Integer::intValue).max().orElse(ZERO_SCORE);
    double median = calculateMedian(scores);

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

  private static PlayerScore emptyPlayerScore(
      UUID playerId, String playerName, String playerRegion) {
    return PlayerScore.builder()
        .playerId(playerId)
        .playerName(playerName)
        .playerRegion(playerRegion)
        .totalPoints(ZERO_SCORE)
        .scoreCount(ZERO_SCORE)
        .averageScore(0.0)
        .minScore(ZERO_SCORE)
        .maxScore(ZERO_SCORE)
        .medianScore(0.0)
        .build();
  }

  private static double calculateMedian(Collection<Integer> scores) {
    List<Integer> sortedScores = scores.stream().sorted().toList();
    int size = sortedScores.size();
    if (size % MEDIAN_DIVISOR == ZERO_SCORE) {
      int upperIndex = size / MEDIAN_DIVISOR;
      int lowerIndex = upperIndex - INDEX_OFFSET;
      int middlePairSum = sortedScores.get(lowerIndex) + sortedScores.get(upperIndex);
      return middlePairSum / EVEN_MEDIAN_DENOMINATOR;
    }
    return sortedScores.get(size / MEDIAN_DIVISOR);
  }

  /** Detailed score values for one player. */
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
  }
}
