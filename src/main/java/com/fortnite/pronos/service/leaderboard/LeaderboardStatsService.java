package com.fortnite.pronos.service.leaderboard;

import java.util.*;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.dto.LeaderboardStatsDTO;
import com.fortnite.pronos.model.*;
import com.fortnite.pronos.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for leaderboard statistics and distributions.
 *
 * <p>Extracted from LeaderboardService to respect SRP (Single Responsibility Principle) and
 * CLAUDE.md 500-line limit.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaderboardStatsService {

  private final TeamRepository teamRepository;
  private final ScoreRepository scoreRepository;
  private final PlayerRepository playerRepository;

  /** Obtenir les statistiques du leaderboard - VERSION OPTIMISÉE SANS N+1 */
  @Cacheable(value = "gameStats", key = "'stats_default'")
  public LeaderboardStatsDTO getLeaderboardStats() {
    return getLeaderboardStats(2025); // Utiliser saison 2025 par défaut, cohérent avec le front-end
  }

  /** Obtenir les statistiques du leaderboard pour une saison spécifique */
  @Cacheable(value = "gameStats", key = "#season")
  public LeaderboardStatsDTO getLeaderboardStats(int season) {
    log.info("[STATS] Calcul des statistiques pour la saison {}", season);

    List<Team> teams = teamRepository.findBySeason(season);
    log.info("[DATA] Equipes trouvees: {}", teams.size());

    // OPTIMISATION: Une seule requête pour tous les joueurs
    List<Player> allPlayers = playerRepository.findAll();
    int totalPlayers = allPlayers.size();
    log.info("[DATA] Total joueurs trouves: {}", totalPlayers);

    // OPTIMISATION CRITIQUE: Récupérer TOUS les scores en UNE SEULE requête
    Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(season);
    log.info("[PERF] Scores recuperes: {} joueurs ont des scores", playerPointsMap.size());

    int totalTeams = teams.size();
    long totalPoints = 0;
    Map<String, Long> regionPoints = new HashMap<>();

    // Calculer les points totaux et les stats régionales EN MÉMOIRE
    for (Player player : allPlayers) {
      String region = player.getRegion().name();
      Integer playerPoints = playerPointsMap.getOrDefault(player.getId(), 0);
      long points = playerPoints != null ? playerPoints.longValue() : 0L;

      totalPoints += points;
      regionPoints.merge(region, points, Long::sum);
    }

    // Calculer la moyenne des points par équipe
    double averagePoints = totalTeams > 0 ? (double) totalPoints / totalTeams : 0.0;

    log.info(
        "[OK] Stats calculees pour saison {} - {} equipes, {} joueurs total, {} points total",
        season,
        totalTeams,
        totalPlayers,
        totalPoints);

    return LeaderboardStatsDTO.builder()
        .totalTeams(totalTeams)
        .totalPlayers(totalPlayers)
        .totalPoints(totalPoints)
        .averagePoints(averagePoints)
        .regionStats(regionPoints)
        .build();
  }

  /** Obtenir les statistiques du leaderboard pour une game spécifique */
  public LeaderboardStatsDTO getLeaderboardStatsByGame(UUID gameId) {
    log.info("[STATS] Calcul des statistiques pour la game {}", gameId);

    List<Team> teams = teamRepository.findByGameIdWithFetch(gameId);
    log.info("[DATA] Equipes trouvees pour la game: {}", teams.size());

    if (teams.isEmpty()) {
      return LeaderboardStatsDTO.builder()
          .totalTeams(0)
          .totalPlayers(0)
          .totalPoints(0L)
          .averagePoints(0.0)
          .regionStats(new HashMap<>())
          .build();
    }

    // Extraire les joueurs des équipes de cette game
    Set<UUID> playerIds = new HashSet<>();
    for (Team team : teams) {
      for (TeamPlayer tp : team.getPlayers()) {
        if (tp.isActive()) {
          playerIds.add(tp.getPlayer().getId());
        }
      }
    }

    List<Player> gamePlayers = playerRepository.findAllById(playerIds);
    int totalPlayers = gamePlayers.size();

    Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(2025);

    int totalTeams = teams.size();
    long totalPoints = 0;
    Map<String, Long> regionPoints = new HashMap<>();

    for (Player player : gamePlayers) {
      String region = player.getRegion().name();
      Integer playerPoints = playerPointsMap.getOrDefault(player.getId(), 0);
      long points = playerPoints != null ? playerPoints.longValue() : 0L;

      totalPoints += points;
      regionPoints.merge(region, points, Long::sum);
    }

    double averagePoints = totalTeams > 0 ? (double) totalPoints / totalTeams : 0.0;

    log.info(
        "[OK] Stats calculees pour game {} - {} equipes, {} joueurs, {} points",
        gameId,
        totalTeams,
        totalPlayers,
        totalPoints);

    return LeaderboardStatsDTO.builder()
        .totalTeams(totalTeams)
        .totalPlayers(totalPlayers)
        .totalPoints(totalPoints)
        .averagePoints(averagePoints)
        .regionStats(regionPoints)
        .build();
  }

  /** Obtenir la répartition par région de tous les joueurs */
  @Cacheable(value = "regionDistribution", key = "'all_regions'")
  public Map<String, Integer> getRegionDistribution() {
    List<Player> allPlayers = playerRepository.findAll();
    Map<String, Integer> regionCounts = new HashMap<>();

    for (Player player : allPlayers) {
      String region = player.getRegion().name();
      regionCounts.merge(region, 1, Integer::sum);
    }

    log.info("[DATA] Repartition par region: {}", regionCounts);
    return regionCounts;
  }

  /** Obtenir la répartition par région pour une game spécifique */
  public Map<String, Integer> getRegionDistributionByGame(UUID gameId) {
    List<Team> teams = teamRepository.findByGameIdWithFetch(gameId);

    Set<UUID> playerIds = new HashSet<>();
    for (Team team : teams) {
      for (TeamPlayer tp : team.getPlayers()) {
        if (tp.isActive()) {
          playerIds.add(tp.getPlayer().getId());
        }
      }
    }

    List<Player> gamePlayers = playerRepository.findAllById(playerIds);
    Map<String, Integer> regionCounts = new HashMap<>();

    for (Player player : gamePlayers) {
      String region = player.getRegion().name();
      regionCounts.merge(region, 1, Integer::sum);
    }

    log.info("[DATA] Repartition par region pour game {}: {}", gameId, regionCounts);
    return regionCounts;
  }

  /** Obtenir la répartition par tranche de tous les joueurs */
  @Cacheable(value = "regionDistribution", key = "'all_tranches'")
  public Map<String, Integer> getTrancheDistribution() {
    List<Player> allPlayers = playerRepository.findAll();
    Map<String, Integer> trancheCounts = new HashMap<>();

    for (Player player : allPlayers) {
      String tranche = "Tranche " + player.getTranche();
      trancheCounts.merge(tranche, 1, Integer::sum);
    }

    log.info("[DATA] Repartition par tranche: {}", trancheCounts);
    return trancheCounts;
  }
}
