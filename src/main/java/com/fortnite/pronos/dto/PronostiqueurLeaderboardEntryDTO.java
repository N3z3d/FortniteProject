package com.fortnite.pronos.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PronostiqueurLeaderboardEntryDTO {
  private UUID userId;
  private String username;
  private String email;
  private int rank;
  private int totalPoints;
  private int totalTeams;
  private int avgPointsPerTeam;
  private int bestTeamPoints;
  private String bestTeamName;
  private int victories; // Nombre de fois dans le top 3
  private double winRate; // Pourcentage de victoires
}
