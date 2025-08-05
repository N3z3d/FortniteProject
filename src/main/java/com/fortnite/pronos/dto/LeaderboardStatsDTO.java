package com.fortnite.pronos.dto;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

/** DTO pour les statistiques du leaderboard */
@Data
@Builder
public class LeaderboardStatsDTO {
  private int totalTeams;
  private int totalPlayers;
  private long totalPoints;
  private double averagePoints;
  private Map<String, Long> regionStats;
}
