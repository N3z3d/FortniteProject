package com.fortnite.pronos.dto;

import java.util.List;

import com.fortnite.pronos.dto.leaderboard.TeamInfoDto;
import com.fortnite.pronos.model.Player;

import lombok.Data;

@Data
public class PlayerLeaderboardEntryDTO {
  private String playerId;
  private String nickname;
  private String username;
  private Player.Region region;
  private String tranche;
  private int rank;
  private int totalPoints;
  private double avgPointsPerGame;
  private int bestScore;
  private int teamsCount;
  private List<TeamInfoDto> teams;
  private List<String> pronostiqueurs;
}
