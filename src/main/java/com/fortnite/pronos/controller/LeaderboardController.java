package com.fortnite.pronos.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fortnite.pronos.dto.LeaderboardEntryDTO;
import com.fortnite.pronos.dto.LeaderboardStatsDTO;
import com.fortnite.pronos.dto.PlayerLeaderboardEntryDTO;
import com.fortnite.pronos.dto.PronostiqueurLeaderboardEntryDTO;
import com.fortnite.pronos.service.leaderboard.LeaderboardDebugService;
import com.fortnite.pronos.service.leaderboard.LeaderboardStatsService;
import com.fortnite.pronos.service.leaderboard.PlayerLeaderboardService;
import com.fortnite.pronos.service.leaderboard.PronostiqueurLeaderboardService;
import com.fortnite.pronos.service.leaderboard.TeamLeaderboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class LeaderboardController {

  private final TeamLeaderboardService teamLeaderboardService;
  private final PlayerLeaderboardService playerLeaderboardService;
  private final PronostiqueurLeaderboardService pronostiqueurLeaderboardService;
  private final LeaderboardStatsService statsService;
  private final LeaderboardDebugService debugService;

  /** Obtenir le leaderboard complet */
  @GetMapping
  public ResponseEntity<List<LeaderboardEntryDTO>> getLeaderboard(
      @RequestParam(defaultValue = "2025") Integer season,
      @RequestParam(required = false) String region,
      @RequestParam(required = false) String gameId) {

    log.info(
        "[LEADERBOARD] Demande leaderboard - Saison: {}, Région: {}, GameId: {}",
        season,
        region,
        gameId);

    try {
      List<LeaderboardEntryDTO> entries;

      // Si gameId est fourni, filtrer par game (priorité)
      if (gameId != null && !gameId.trim().isEmpty()) {
        entries = teamLeaderboardService.getLeaderboardByGame(UUID.fromString(gameId));
      } else {
        entries = teamLeaderboardService.getLeaderboard(season);
      }

      // Filtrer par région si nécessaire
      if (region != null && !region.trim().isEmpty()) {
        entries =
            entries.stream()
                .filter(
                    entry ->
                        entry.getPlayers().stream()
                            .anyMatch(
                                player ->
                                    player.getRegion().name().equalsIgnoreCase(region.trim())))
                .toList();
        log.info("[FILTER] Filtrage par région {} -> {} équipes", region, entries.size());
      }

      log.info("[OK] Leaderboard retourné: {} équipes", entries.size());
      return ResponseEntity.ok(entries);

    } catch (Exception e) {
      log.error("[ERROR] Erreur lors de la récupération du leaderboard", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** Obtenir le leaderboard pour une saison spécifique (compatibilité frontend) */
  @GetMapping("/season/{season}")
  public ResponseEntity<List<LeaderboardEntryDTO>> getLeaderboardBySeason(
      @PathVariable Integer season,
      @RequestParam(required = false) String region,
      @RequestParam(required = false) String gameId) {

    log.info(
        "[LEADERBOARD] Demande leaderboard par path - Saison: {}, Région: {}, GameId: {}",
        season,
        region,
        gameId);
    return getLeaderboard(season, region, gameId);
  }

  /** Obtenir le classement d'une équipe spécifique */
  @GetMapping("/team/{teamId}")
  public ResponseEntity<LeaderboardEntryDTO> getTeamRanking(@PathVariable String teamId) {
    log.info("Récupération du classement pour l'équipe: {}", teamId);

    try {
      LeaderboardEntryDTO entry = teamLeaderboardService.getTeamRanking(teamId);
      return ResponseEntity.ok(entry);
    } catch (Exception e) {
      log.error("Erreur lors de la récupération du classement de l'équipe", e);
      return ResponseEntity.notFound().build();
    }
  }

  /** Obtenir les statistiques du leaderboard */
  @GetMapping("/stats")
  public ResponseEntity<LeaderboardStatsDTO> getLeaderboardStats(
      @RequestParam(defaultValue = "2025") Integer season,
      @RequestParam(required = false) String gameId) {
    log.info("Récupération des statistiques - Saison: {}, GameId: {}", season, gameId);

    try {
      LeaderboardStatsDTO stats;

      if (gameId != null && !gameId.trim().isEmpty()) {
        stats = statsService.getLeaderboardStatsByGame(UUID.fromString(gameId));
      } else {
        stats = statsService.getLeaderboardStats(season);
      }

      return ResponseEntity.ok(stats);
    } catch (Exception e) {
      log.error("Erreur lors de la récupération des statistiques", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** Obtenir la répartition par région de tous les joueurs */
  @GetMapping("/distribution/regions")
  public ResponseEntity<Map<String, Integer>> getRegionDistribution(
      @RequestParam(required = false) String gameId) {
    log.info("Récupération de la répartition par région - GameId: {}", gameId);

    try {
      Map<String, Integer> distribution;

      if (gameId != null && !gameId.trim().isEmpty()) {
        distribution = statsService.getRegionDistributionByGame(UUID.fromString(gameId));
      } else {
        distribution = statsService.getRegionDistribution();
      }

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
      Map<String, Integer> distribution = statsService.getTrancheDistribution();
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

    log.info("[LEADERBOARD] Demande classement pronostiqueurs - Saison: {}", season);

    try {
      List<PronostiqueurLeaderboardEntryDTO> entries =
          pronostiqueurLeaderboardService.getPronostiqueurLeaderboard(season);
      log.info("[OK] Classement pronostiqueurs retourné: {} utilisateurs", entries.size());
      return ResponseEntity.ok(entries);

    } catch (Exception e) {
      log.error("[ERROR] Erreur lors de la récupération du classement des pronostiqueurs", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** Obtenir le classement des joueurs Fortnite */
  @GetMapping("/joueurs")
  public ResponseEntity<List<PlayerLeaderboardEntryDTO>> getPlayerLeaderboard(
      @RequestParam(defaultValue = "2025") Integer season,
      @RequestParam(required = false) String region,
      @RequestParam(required = false) String gameId) {

    log.info(
        "[PLAYER] Demande classement joueurs - Saison: {}, Région: {}, GameId: {}",
        season,
        region,
        gameId);

    try {
      List<PlayerLeaderboardEntryDTO> entries;

      // Si gameId est fourni, filtrer par game
      if (gameId != null && !gameId.trim().isEmpty()) {
        entries = playerLeaderboardService.getPlayerLeaderboardByGame(UUID.fromString(gameId));
      } else {
        entries = playerLeaderboardService.getPlayerLeaderboard(season);
      }

      // Filtrer par région si nécessaire
      if (region != null && !region.trim().isEmpty()) {
        entries =
            entries.stream()
                .filter(entry -> entry.getRegion().name().equalsIgnoreCase(region.trim()))
                .toList();
        log.info("[FILTER] Filtrage par région {} -> {} joueurs", region, entries.size());
      }

      log.info("[OK] Classement joueurs retourné: {} joueurs", entries.size());
      return ResponseEntity.ok(entries);

    } catch (Exception e) {
      log.error("[ERROR] Erreur lors de la récupération du classement des joueurs", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /** Debug endpoint pour vérifier pourquoi les stats sont à 0 */
  @GetMapping("/debug/stats")
  public ResponseEntity<Map<String, Object>> getDebugStats() {
    log.info("[DEBUG] Debug des statistiques du leaderboard");

    int season = new Date().getYear() + 1900;

    try {
      Map<String, Object> debug = debugService.getDebugStats(season);
      log.info("[DEBUG] Debug result: {}", debug);
      return ResponseEntity.ok(debug);

    } catch (Exception e) {
      log.error("[ERROR] Erreur lors du debug des statistiques", e);
      Map<String, Object> debug = new HashMap<>();
      debug.put("error", e.getMessage());
      return ResponseEntity.ok(debug);
    }
  }

  /** Debug simple pour vérifier les données de base */
  @GetMapping("/debug/simple")
  public ResponseEntity<Map<String, Object>> getDebugSimple() {
    try {
      return ResponseEntity.ok(debugService.getDebugSimple());

    } catch (Exception e) {
      log.error("[ERROR] Erreur lors du debug simple", e);
      Map<String, Object> debug = new HashMap<>();
      debug.put("error", e.getMessage());
      return ResponseEntity.ok(debug);
    }
  }
}
