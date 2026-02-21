package com.fortnite.pronos.service.leaderboard;

import java.util.*;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.dto.PlayerLeaderboardEntryDTO;
import com.fortnite.pronos.dto.leaderboard.TeamInfoDto;

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
@SuppressWarnings({"java:S112"})
public class PlayerLeaderboardService {

  private final com.fortnite.pronos.repository.PlayerRepository playerRepository;
  private final com.fortnite.pronos.repository.ScoreRepository scoreRepository;
  private final com.fortnite.pronos.repository.TeamRepository teamRepository;

  /** Obtenir le classement des joueurs Fortnite */
  @Cacheable(value = "playerScores", key = "'players_' + #season")
  public List<PlayerLeaderboardEntryDTO> getPlayerLeaderboard(int season) {
    log.info("[PLAYER] Recuperation du classement des joueurs - Saison: {}", season);

    try {
      // 1. RÃ©cupÃ©rer tous les joueurs
      List<com.fortnite.pronos.model.Player> players = playerRepository.findAll();
      log.info("[DATA] {} joueurs trouves", players.size());

      // 2. RÃ©cupÃ©rer tous les scores groupÃ©s par joueur
      Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(season);
      log.info("[DATA] {} scores trouves pour la saison {}", playerPointsMap.size(), season);

      // 3. RÃ©cupÃ©rer toutes les Ã©quipes avec leurs joueurs pour cette saison
      List<com.fortnite.pronos.model.Team> teams = teamRepository.findBySeasonWithFetch(season);
      Map<UUID, List<TeamInfoDto>> playerTeamsMap = new HashMap<>();
      Map<UUID, List<String>> playerPronostiqueurMap = new HashMap<>();

      // Construire les maps des Ã©quipes et pronostiqueurs par joueur
      for (com.fortnite.pronos.model.Team team : teams) {
        for (com.fortnite.pronos.model.TeamPlayer teamPlayer : team.getPlayers()) {
          if (teamPlayer.isActive()) {
            UUID playerId = teamPlayer.getPlayer().getId();

            // Ajouter l'Ã©quipe
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

      // 4. CrÃ©er les entrÃ©es pour chaque joueur
      List<PlayerLeaderboardEntryDTO> entries = new ArrayList<>();

      for (com.fortnite.pronos.model.Player player : players) {
        UUID playerId = player.getId();
        int totalPoints = playerPointsMap.getOrDefault(playerId, 0);

        // Calculer les statistiques de base
        double avgPointsPerGame =
            totalPoints > 0 ? (double) totalPoints / Math.max(1, totalPoints / 1000) : 0.0;
        int bestScore = totalPoints; // Simplification : on utilise le total comme meilleur score

        // RÃ©cupÃ©rer les Ã©quipes et pronostiqueurs
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

      // 5. Trier par points dÃ©croissants et assigner les rangs
      entries.sort((a, b) -> Integer.compare(b.getTotalPoints(), a.getTotalPoints()));

      for (int i = 0; i < entries.size(); i++) {
        entries.get(i).setRank(i + 1);
      }

      log.info("[OK] Classement joueurs gÃ©nÃ©rÃ© avec {} joueurs", entries.size());
      return entries;

    } catch (Exception e) {
      log.error("[ERROR] Erreur lors de la gÃ©nÃ©ration du classement des joueurs", e);
      throw new RuntimeException("Erreur lors de la gÃ©nÃ©ration du classement des joueurs", e);
    }
  }

  /** Obtenir le classement des joueurs Fortnite pour une game spÃ©cifique */
  public List<PlayerLeaderboardEntryDTO> getPlayerLeaderboardByGame(UUID gameId) {
    log.info("[PLAYER] Recuperation du classement des joueurs pour la game {}", gameId);

    try {
      // 1. RÃ©cupÃ©rer les Ã©quipes de cette game avec leurs joueurs
      List<com.fortnite.pronos.model.Team> teams = teamRepository.findByGameIdWithFetch(gameId);
      log.info("[DATA] {} equipes trouvees pour la game {}", teams.size(), gameId);

      if (teams.isEmpty()) {
        log.warn("[WARN] Aucune Ã©quipe trouvÃ©e pour la game {}", gameId);
        return new ArrayList<>();
      }

      // 2. Extraire tous les joueurs uniques des Ã©quipes
      Set<UUID> playerIds = new HashSet<>();
      Map<UUID, List<TeamInfoDto>> playerTeamsMap = new HashMap<>();
      Map<UUID, List<String>> playerPronostiqueurMap = new HashMap<>();

      for (com.fortnite.pronos.model.Team team : teams) {
        for (com.fortnite.pronos.model.TeamPlayer teamPlayer : team.getPlayers()) {
          if (teamPlayer.isActive()) {
            UUID playerId = teamPlayer.getPlayer().getId();
            playerIds.add(playerId);

            // Ajouter l'Ã©quipe
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

      log.info("[DATA] {} joueurs uniques trouves dans les equipes", playerIds.size());

      // 3. RÃ©cupÃ©rer les informations des joueurs
      List<com.fortnite.pronos.model.Player> players = playerRepository.findAllById(playerIds);

      // 4. RÃ©cupÃ©rer les scores (saison 2025 par dÃ©faut)
      Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(2025);

      // 5. CrÃ©er les entrÃ©es pour chaque joueur
      List<PlayerLeaderboardEntryDTO> entries = new ArrayList<>();

      for (com.fortnite.pronos.model.Player player : players) {
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

      // 6. Trier par points dÃ©croissants et assigner les rangs
      entries.sort((a, b) -> Integer.compare(b.getTotalPoints(), a.getTotalPoints()));

      for (int i = 0; i < entries.size(); i++) {
        entries.get(i).setRank(i + 1);
      }

      log.info(
          "[OK] Classement joueurs pour game {} gÃ©nÃ©rÃ© avec {} joueurs", gameId, entries.size());
      return entries;

    } catch (Exception e) {
      log.error(
          "[ERROR] Erreur lors de la gÃ©nÃ©ration du classement des joueurs pour la game {}",
          gameId,
          e);
      throw new RuntimeException("Erreur lors de la gÃ©nÃ©ration du classement des joueurs", e);
    }
  }
}
