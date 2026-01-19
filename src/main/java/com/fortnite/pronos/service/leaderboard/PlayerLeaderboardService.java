package com.fortnite.pronos.service.leaderboard;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.dto.PlayerLeaderboardEntryDTO;
import com.fortnite.pronos.dto.leaderboard.TeamInfoDto;
import com.fortnite.pronos.model.*;
import com.fortnite.pronos.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for Fortnite player leaderboard operations.
 *
 * <p>Extracted from LeaderboardService to respect SRP (Single Responsibility Principle) and
 * CLAUDE.md 500-line limit.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerLeaderboardService {

  private final PlayerRepository playerRepository;
  private final ScoreRepository scoreRepository;
  private final TeamRepository teamRepository;

  /** Obtenir le classement des joueurs Fortnite */
  @Cacheable(value = "playerScores", key = "'players_' + #season")
  public List<PlayerLeaderboardEntryDTO> getPlayerLeaderboard(int season) {
    log.info("🎮 Récupération du classement des joueurs - Saison: {}", season);

    try {
      // 1. Récupérer tous les joueurs
      List<Player> players = playerRepository.findAll();
      log.info("📊 {} joueurs trouvés", players.size());

      // 2. Récupérer tous les scores groupés par joueur
      Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(season);
      log.info("📊 {} scores trouvés pour la saison {}", playerPointsMap.size(), season);

      // 3. Récupérer toutes les équipes avec leurs joueurs pour cette saison
      List<Team> teams = teamRepository.findBySeasonWithFetch(season);
      Map<UUID, List<TeamInfoDto>> playerTeamsMap = new HashMap<>();
      Map<UUID, List<String>> playerPronostiqueurMap = new HashMap<>();

      // Construire les maps des équipes et pronostiqueurs par joueur
      for (Team team : teams) {
        for (TeamPlayer teamPlayer : team.getPlayers()) {
          if (teamPlayer.isActive()) {
            UUID playerId = teamPlayer.getPlayer().getId();

            // Ajouter l'équipe
            playerTeamsMap
                .computeIfAbsent(playerId, k -> new ArrayList<>())
                .add(
                    new TeamInfoDto(
                        team.getId().toString(), team.getName(), team.getOwner().getUsername()));

            // Ajouter le pronostiqueur
            playerPronostiqueurMap
                .computeIfAbsent(playerId, k -> new ArrayList<>())
                .add(team.getOwner().getUsername());
          }
        }
      }

      // 4. Créer les entrées pour chaque joueur
      List<PlayerLeaderboardEntryDTO> entries = new ArrayList<>();

      for (Player player : players) {
        UUID playerId = player.getId();
        int totalPoints = playerPointsMap.getOrDefault(playerId, 0);

        // Calculer les statistiques de base
        double avgPointsPerGame =
            totalPoints > 0 ? (double) totalPoints / Math.max(1, totalPoints / 1000) : 0.0;
        int bestScore = totalPoints; // Simplification : on utilise le total comme meilleur score

        // Récupérer les équipes et pronostiqueurs
        List<TeamInfoDto> playerTeams = playerTeamsMap.getOrDefault(playerId, new ArrayList<>());
        List<String> playerPronostiqueurs =
            playerPronostiqueurMap.getOrDefault(playerId, new ArrayList<>());

        PlayerLeaderboardEntryDTO entry = new PlayerLeaderboardEntryDTO();
        entry.setPlayerId(playerId.toString());
        entry.setNickname(player.getNickname());
        entry.setUsername(player.getUsername());
        entry.setRegion(player.getRegion());
        entry.setTranche(player.getTranche());
        entry.setTotalPoints(totalPoints);
        entry.setAvgPointsPerGame(avgPointsPerGame);
        entry.setBestScore(bestScore);
        entry.setTeamsCount(playerTeams.size());
        entry.setTeams(playerTeams);
        entry.setPronostiqueurs(
            playerPronostiqueurs.stream().distinct().collect(Collectors.toList()));

        entries.add(entry);
      }

      // 5. Trier par points décroissants et assigner les rangs
      entries.sort((a, b) -> Integer.compare(b.getTotalPoints(), a.getTotalPoints()));

      for (int i = 0; i < entries.size(); i++) {
        entries.get(i).setRank(i + 1);
      }

      log.info("✅ Classement joueurs généré avec {} joueurs", entries.size());
      return entries;

    } catch (Exception e) {
      log.error("❌ Erreur lors de la génération du classement des joueurs", e);
      throw new RuntimeException("Erreur lors de la génération du classement des joueurs", e);
    }
  }

  /** Obtenir le classement des joueurs Fortnite pour une game spécifique */
  public List<PlayerLeaderboardEntryDTO> getPlayerLeaderboardByGame(UUID gameId) {
    log.info("🎮 Récupération du classement des joueurs pour la game {}", gameId);

    try {
      // 1. Récupérer les équipes de cette game avec leurs joueurs
      List<Team> teams = teamRepository.findByGameIdWithFetch(gameId);
      log.info("📊 {} équipes trouvées pour la game {}", teams.size(), gameId);

      if (teams.isEmpty()) {
        log.warn("⚠️ Aucune équipe trouvée pour la game {}", gameId);
        return new ArrayList<>();
      }

      // 2. Extraire tous les joueurs uniques des équipes
      Set<UUID> playerIds = new HashSet<>();
      Map<UUID, List<TeamInfoDto>> playerTeamsMap = new HashMap<>();
      Map<UUID, List<String>> playerPronostiqueurMap = new HashMap<>();

      for (Team team : teams) {
        for (TeamPlayer teamPlayer : team.getPlayers()) {
          if (teamPlayer.isActive()) {
            UUID playerId = teamPlayer.getPlayer().getId();
            playerIds.add(playerId);

            // Ajouter l'équipe
            playerTeamsMap
                .computeIfAbsent(playerId, k -> new ArrayList<>())
                .add(
                    new TeamInfoDto(
                        team.getId().toString(), team.getName(), team.getOwner().getUsername()));

            // Ajouter le pronostiqueur
            playerPronostiqueurMap
                .computeIfAbsent(playerId, k -> new ArrayList<>())
                .add(team.getOwner().getUsername());
          }
        }
      }

      log.info("📊 {} joueurs uniques trouvés dans les équipes", playerIds.size());

      // 3. Récupérer les informations des joueurs
      List<Player> players = playerRepository.findAllById(playerIds);

      // 4. Récupérer les scores (saison 2025 par défaut)
      Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(2025);

      // 5. Créer les entrées pour chaque joueur
      List<PlayerLeaderboardEntryDTO> entries = new ArrayList<>();

      for (Player player : players) {
        UUID playerId = player.getId();
        int totalPoints = playerPointsMap.getOrDefault(playerId, 0);

        double avgPointsPerGame =
            totalPoints > 0 ? (double) totalPoints / Math.max(1, totalPoints / 1000) : 0.0;
        int bestScore = totalPoints;

        List<TeamInfoDto> playerTeams = playerTeamsMap.getOrDefault(playerId, new ArrayList<>());
        List<String> playerPronostiqueurs =
            playerPronostiqueurMap.getOrDefault(playerId, new ArrayList<>());

        PlayerLeaderboardEntryDTO entry = new PlayerLeaderboardEntryDTO();
        entry.setPlayerId(playerId.toString());
        entry.setNickname(player.getNickname());
        entry.setUsername(player.getUsername());
        entry.setRegion(player.getRegion());
        entry.setTranche(player.getTranche());
        entry.setTotalPoints(totalPoints);
        entry.setAvgPointsPerGame(avgPointsPerGame);
        entry.setBestScore(bestScore);
        entry.setTeamsCount(playerTeams.size());
        entry.setTeams(playerTeams);
        entry.setPronostiqueurs(playerPronostiqueurs.stream().distinct().toList());

        entries.add(entry);
      }

      // 6. Trier par points décroissants et assigner les rangs
      entries.sort((a, b) -> Integer.compare(b.getTotalPoints(), a.getTotalPoints()));

      for (int i = 0; i < entries.size(); i++) {
        entries.get(i).setRank(i + 1);
      }

      log.info("✅ Classement joueurs pour game {} généré avec {} joueurs", gameId, entries.size());
      return entries;

    } catch (Exception e) {
      log.error(
          "❌ Erreur lors de la génération du classement des joueurs pour la game {}", gameId, e);
      throw new RuntimeException("Erreur lors de la génération du classement des joueurs", e);
    }
  }
}
