package com.fortnite.pronos.dto.leaderboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Team information for player leaderboard entries.
 *
 * <p>Data Transfer Object for team summary in leaderboard context.
 */
@Getter
@AllArgsConstructor
public class TeamInfoDto {
  private String id;
  private String name;
  private String ownerUsername;
}
