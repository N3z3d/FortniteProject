package com.fortnite.pronos.dto.leaderboard;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Data;

@Data
public class LeaderboardEntryDto {
  private int rank;
  private UUID userId;
  private String username;
  private String userEmail;
  private int totalPoints;
  private Map<String, Integer> pointsByRegion;
  private int regionsWon;
  private int firstPlacePlayers;
  private int worldChampions;
  private TeamInfoDto team;
  private List<RecentMovementDto> recentMovements;

  @Data
  public static class TeamInfoDto {
    private UUID id;
    private String name;
    private int season;
    private int tradesRemaining;
    private OffsetDateTime lastTradeDate;
    private List<PlayerInfoDto> players;
  }

  @Data
  public static class PlayerInfoDto {
    private UUID id;
    private String nickname;
    private String region;
    private String tranche;
    private int points;
    private boolean isActive;
    private int rank;
    private boolean isWorldChampion;
    private OffsetDateTime lastUpdate;
  }

  @Data
  public static class RecentMovementDto {
    private String id;
    private String type;
    private String description;
    private OffsetDateTime timestamp;
  }
}
