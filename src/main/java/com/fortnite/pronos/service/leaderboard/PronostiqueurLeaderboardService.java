package com.fortnite.pronos.service.leaderboard;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.dto.PronostiqueurLeaderboardEntryDTO;
import com.fortnite.pronos.model.*;
import com.fortnite.pronos.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for predictor (user) leaderboard operations.
 *
 * <p>Extracted from LeaderboardService to respect SRP (Single Responsibility Principle) and
 * CLAUDE.md 500-line limit.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PronostiqueurLeaderboardService {

  private final TeamRepository teamRepository;
  private final ScoreRepository scoreRepository;

  /** Obtenir le classement des pronostiqueurs */
  @Cacheable(value = "leaderboard", key = "'pronostiqueurs_' + #season")
  public List<PronostiqueurLeaderboardEntryDTO> getPronostiqueurLeaderboard(int season) {
    log.info("👥 Récupération du classement des pronostiqueurs - Saison: {}", season);

    // 1. Récupérer toutes les équipes avec leurs propriétaires
    List<Team> teams = teamRepository.findBySeasonWithFetch(season);

    // 2. Récupérer tous les scores en une seule requête
    Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(season);

    // 3. Grouper par pronostiqueur et calculer les statistiques
    Map<UUID, List<Team>> teamsByUser =
        teams.stream().collect(Collectors.groupingBy(team -> team.getOwner().getId()));

    List<PronostiqueurLeaderboardEntryDTO> entries = new ArrayList<>();

    for (Map.Entry<UUID, List<Team>> entry : teamsByUser.entrySet()) {
      UUID userId = entry.getKey();
      List<Team> userTeams = entry.getValue();
      User user = userTeams.get(0).getOwner(); // Récupérer l'utilisateur

      int totalPoints = 0;
      int bestTeamPoints = 0;
      String bestTeamName = "";

      // Calculer les points pour chaque équipe de l'utilisateur
      for (Team team : userTeams) {
        int teamPoints = 0;
        for (TeamPlayer teamPlayer : team.getPlayers()) {
          if (teamPlayer.isActive()) {
            teamPoints += playerPointsMap.getOrDefault(teamPlayer.getPlayer().getId(), 0);
          }
        }
        totalPoints += teamPoints;

        if (teamPoints > bestTeamPoints) {
          bestTeamPoints = teamPoints;
          bestTeamName = team.getName();
        }
      }

      int avgPointsPerTeam = userTeams.size() > 0 ? totalPoints / userTeams.size() : 0;

      entries.add(
          PronostiqueurLeaderboardEntryDTO.builder()
              .userId(userId)
              .username(user.getUsername())
              .email(user.getEmail())
              .totalPoints(totalPoints)
              .totalTeams(userTeams.size())
              .avgPointsPerTeam(avgPointsPerTeam)
              .bestTeamPoints(bestTeamPoints)
              .bestTeamName(bestTeamName)
              .victories(0) // À calculer selon les critères de victoire
              .winRate(0.0) // À calculer selon les critères de victoire
              .build());
    }

    // 4. Trier par points décroissants et assigner les rangs
    entries.sort((a, b) -> Integer.compare(b.getTotalPoints(), a.getTotalPoints()));

    for (int i = 0; i < entries.size(); i++) {
      entries.get(i).setRank(i + 1);
    }

    log.info("✅ Classement pronostiqueurs généré avec {} utilisateurs", entries.size());
    return entries;
  }
}
