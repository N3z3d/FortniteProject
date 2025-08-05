package com.fortnite.pronos.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fortnite.pronos.dto.LeaderboardEntryDTO;
import com.fortnite.pronos.dto.LeaderboardStatsDTO;
import com.fortnite.pronos.dto.PlayerLeaderboardEntryDTO;
import com.fortnite.pronos.dto.PronostiqueurLeaderboardEntryDTO;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.ScoreRepository;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.service.LeaderboardService;
import com.fortnite.pronos.service.cache.LeaderboardCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class LeaderboardController {

  private final LeaderboardService leaderboardService;
  private final LeaderboardCacheService leaderboardCacheService;
  private final ScoreRepository scoreRepository;
  private final PlayerRepository playerRepository;
  private final TeamRepository teamRepository;

  /** Obtenir le leaderboard complet */
  @GetMapping
  public ResponseEntity<List<LeaderboardEntryDTO>> getLeaderboard(
      @RequestParam(defaultValue = "2025") Integer season,
      @RequestParam(required = false) String region) {

    log.info("📊 Demande leaderboard - Saison: {}, Région: {}", season, region);

    try {
      // Utiliser la méthode optimisée avec cache
      List<LeaderboardEntryDTO> entries = leaderboardCacheService.getCachedLeaderboard(season);

      // Filtrer par région si nécessaire (côté application pour éviter complexity)
      if (region != null && !region.trim().isEmpty()) {
        entries =
            entries.stream()
                .filter(
                    entry ->
                        entry.getPlayers().stream()
                            .anyMatch(
                                player ->
                                    player.getRegion().name().equalsIgnoreCase(region.trim())))
                .collect(Collectors.toList());
        log.info("🌍 Filtrage par région {} -> {} équipes", region, entries.size());
      }

      log.info("✅ Leaderboard retourné: {} équipes", entries.size());
      return ResponseEntity.ok(entries);

    } catch (Exception e) {
      log.error("❌ Erreur lors de la récupération du leaderboard", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** Obtenir le leaderboard pour une saison spécifique (compatibilité frontend) */
  @GetMapping("/season/{season}")
  public ResponseEntity<List<LeaderboardEntryDTO>> getLeaderboardBySeason(
      @PathVariable Integer season, @RequestParam(required = false) String region) {

    log.info("📊 Demande leaderboard par path - Saison: {}, Région: {}", season, region);
    return getLeaderboard(season, region);
  }

  /** Obtenir le classement d'une équipe spécifique */
  @GetMapping("/team/{teamId}")
  public ResponseEntity<LeaderboardEntryDTO> getTeamRanking(@PathVariable String teamId) {
    log.info("Récupération du classement pour l'équipe: {}", teamId);

    try {
      LeaderboardEntryDTO entry = leaderboardService.getTeamRanking(teamId);
      return ResponseEntity.ok(entry);
    } catch (Exception e) {
      log.error("Erreur lors de la récupération du classement de l'équipe", e);
      return ResponseEntity.notFound().build();
    }
  }

  /** Obtenir les statistiques du leaderboard */
  @GetMapping("/stats")
  public ResponseEntity<LeaderboardStatsDTO> getLeaderboardStats(
      @RequestParam(defaultValue = "2025") Integer season) {
    log.info("Récupération des statistiques du leaderboard pour la saison: {}", season);

    try {
      LeaderboardStatsDTO stats = leaderboardService.getLeaderboardStats(season);
      return ResponseEntity.ok(stats);
    } catch (Exception e) {
      log.error("Erreur lors de la récupération des statistiques", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** Obtenir la répartition par région de tous les joueurs */
  @GetMapping("/distribution/regions")
  public ResponseEntity<Map<String, Integer>> getRegionDistribution() {
    log.info("Récupération de la répartition par région");

    try {
      Map<String, Integer> distribution = leaderboardService.getRegionDistribution();
      return ResponseEntity.ok(distribution);
    } catch (Exception e) {
      log.error("Erreur lors de la récupération de la répartition par région", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** Obtenir la répartition par tranche de tous les joueurs */
  @GetMapping("/distribution/tranches")
  public ResponseEntity<Map<String, Integer>> getTrancheDistribution() {
    log.info("Récupération de la répartition par tranche");

    try {
      Map<String, Integer> distribution = leaderboardService.getTrancheDistribution();
      return ResponseEntity.ok(distribution);
    } catch (Exception e) {
      log.error("Erreur lors de la récupération de la répartition par tranche", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** Obtenir le classement des pronostiqueurs */
  @GetMapping("/pronostiqueurs")
  public ResponseEntity<List<PronostiqueurLeaderboardEntryDTO>> getPronostiqueurLeaderboard(
      @RequestParam(defaultValue = "2025") Integer season) {

    log.info("📊 Demande classement pronostiqueurs - Saison: {}", season);

    try {
      List<PronostiqueurLeaderboardEntryDTO> entries =
          leaderboardService.getPronostiqueurLeaderboard(season);
      log.info("✅ Classement pronostiqueurs retourné: {} utilisateurs", entries.size());
      return ResponseEntity.ok(entries);

    } catch (Exception e) {
      log.error("❌ Erreur lors de la récupération du classement des pronostiqueurs", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** Obtenir le classement des joueurs Fortnite */
  @GetMapping("/joueurs")
  public ResponseEntity<List<PlayerLeaderboardEntryDTO>> getPlayerLeaderboard(
      @RequestParam(defaultValue = "2025") Integer season,
      @RequestParam(required = false) String region) {

    log.info("🎮 Demande classement joueurs - Saison: {}, Région: {}", season, region);

    try {
      List<PlayerLeaderboardEntryDTO> entries = leaderboardService.getPlayerLeaderboard(season);

      // Filtrer par région si nécessaire
      if (region != null && !region.trim().isEmpty()) {
        entries =
            entries.stream()
                .filter(entry -> entry.getRegion().name().equalsIgnoreCase(region.trim()))
                .collect(Collectors.toList());
        log.info("🌍 Filtrage par région {} -> {} joueurs", region, entries.size());
      }

      log.info("✅ Classement joueurs retourné: {} joueurs", entries.size());
      return ResponseEntity.ok(entries);

    } catch (Exception e) {
      log.error("❌ Erreur lors de la récupération du classement des joueurs", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** Debug endpoint pour vérifier pourquoi les stats sont à 0 */
  @GetMapping("/debug/stats")
  public ResponseEntity<Map<String, Object>> getDebugStats() {
    log.info("🔍 Debug des statistiques du leaderboard");

    Map<String, Object> debug = new HashMap<>();
    int season = new Date().getYear() + 1900;

    try {
      // Compter les équipes
      long teamCount = teamRepository.findBySeason(season).size();
      debug.put("totalTeams", teamCount);
      debug.put("season", season);

      // Compter les joueurs
      long playerCount = playerRepository.count();
      debug.put("totalPlayers", playerCount);

      // Compter les scores
      long scoreCount = scoreRepository.count();
      debug.put("totalScores", scoreCount);

      // Tester la requête groupée
      List<Object[]> rawScores = scoreRepository.findAllBySeasonGroupedByPlayerRaw(season);
      debug.put("rawScoresCount", rawScores.size());
      debug.put(
          "rawScoresSample",
          rawScores.stream()
              .limit(3)
              .map(row -> Map.of("playerId", row[0], "totalPoints", row[1]))
              .collect(Collectors.toList()));

      // Tester la map finale
      Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(season);
      debug.put("playerPointsMapSize", playerPointsMap.size());
      debug.put(
          "totalPointsFromMap",
          playerPointsMap.values().stream().mapToInt(Integer::intValue).sum());

      log.info("🔍 Debug result: {}", debug);
      return ResponseEntity.ok(debug);

    } catch (Exception e) {
      log.error("❌ Erreur lors du debug des statistiques", e);
      debug.put("error", e.getMessage());
      return ResponseEntity.ok(debug);
    }
  }

  /** Debug simple pour vérifier les données de base */
  @GetMapping("/debug/simple")
  public ResponseEntity<Map<String, Object>> getDebugSimple() {
    Map<String, Object> debug = new HashMap<>();

    try {
      // Vérifier les données de base
      debug.put("totalPlayers", playerRepository.count());
      debug.put("totalTeams", teamRepository.count());
      debug.put("totalScores", scoreRepository.count());

      // Échantillon de joueurs
      debug.put(
          "playersSample",
          playerRepository.findAll().stream()
              .limit(3)
              .map(
                  player ->
                      Map.of(
                          "id", player.getId(),
                          "nickname", player.getNickname(),
                          "region", player.getRegion().name(),
                          "season", player.getCurrentSeason()))
              .collect(Collectors.toList()));

      // Échantillon de scores
      debug.put(
          "scoresSample",
          scoreRepository.findAll().stream()
              .limit(3)
              .map(
                  score ->
                      Map.of(
                          "playerNickname", score.getPlayer().getNickname(),
                          "season", score.getSeason(),
                          "points", score.getPoints()))
              .collect(Collectors.toList()));

      return ResponseEntity.ok(debug);

    } catch (Exception e) {
      log.error("❌ Erreur lors du debug simple", e);
      debug.put("error", e.getMessage());
      return ResponseEntity.ok(debug);
    }
  }

  private com.fortnite.pronos.model.Player.Region convertStringToRegion(String region) {
    switch (region.toUpperCase()) {
      case "EU":
        return com.fortnite.pronos.model.Player.Region.EU;
      case "NAC":
        return com.fortnite.pronos.model.Player.Region.NAC;
      case "NAW":
        return com.fortnite.pronos.model.Player.Region.NAW;
      case "BR":
        return com.fortnite.pronos.model.Player.Region.BR;
      case "ASIA":
        return com.fortnite.pronos.model.Player.Region.ASIA;
      case "OCE":
        return com.fortnite.pronos.model.Player.Region.OCE;
      case "ME":
        return com.fortnite.pronos.model.Player.Region.ME;
      default:
        return com.fortnite.pronos.model.Player.Region.EU;
    }
  }
}
