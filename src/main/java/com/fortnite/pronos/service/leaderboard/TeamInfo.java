package com.fortnite.pronos.service.leaderboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Team information for player leaderboard entries.
 *
 * <p>Extracted from LeaderboardService inner class to follow Clean Code principles.
 */
@Getter
@AllArgsConstructor
public class TeamInfo {
  private String id;
  private String name;
  private String ownerUsername;
}
