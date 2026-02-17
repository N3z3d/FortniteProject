package com.fortnite.pronos.dto.admin;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityDto {

  private long recentGamesCount;
  private long recentTradesCount;
  private long recentUsersCount;
  private List<ActivityEntry> recentGames;
  private List<ActivityEntry> recentTrades;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ActivityEntry {

    private String id;
    private String name;
    private String status;
    private String createdAt;
  }
}
