package com.fortnite.pronos.service.leaderboard;

import java.util.*;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.dto.LeaderboardEntryDTO;
import com.fortnite.pronos.model.*;
import com.fortnite.pronos.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for team leaderboard operations.
 *
 * <p>Extracted from LeaderboardService to respect SRP (Single Responsibility Principle) and
 * CLAUDE.md 500-line limit.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamLeaderboardService {

  private final TeamRepository teamRepository;
  private final ScoreRepository scoreRepository;
  private final PlayerRepository playerRepository;

  /** Obtenir le leaderboard complet - VERSION OPTIMISÉE SANS N+1 + CACHE */
  @Cacheable(value = "leaderboard", key = "#season", unless = "#result.isEmpty()")
  public List<LeaderboardEntryDTO> getLeaderboard(int season) {
    log.info(
        "[LEADERBOARD] Recuperation du leaderboard pour la saison {} - VERSION OPTIMISEE", season);

    // 1. Récupérer toutes les équipes avec FETCH EAGER optimisé
    List<Team> teams = teamRepository.findBySeasonWithFetch(season);
    log.debug("[DATA] {} equipes trouvees", teams.size());

    // 2. OPTIMISATION: Récupérer tous les scores en UNE SEULE requête
    Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(season);
    log.debug("[PERF] {} scores recuperes en une seule requete optimisee", playerPointsMap.size());

    // 3. Construire le leaderboard sans requêtes supplémentaires
    List<LeaderboardEntryDTO> entries = new ArrayList<>();

    for (Team team : teams) {
      long totalPoints = 0;
      List<LeaderboardEntryDTO.PlayerInfo> playerInfos = new ArrayList<>();

      // Récupérer les joueurs de l'équipe (déjà en mémoire via fetch)
      for (TeamPlayer teamPlayer : team.getPlayers()) {
        if (!teamPlayer.isActive()) continue;

        Player player = teamPlayer.getPlayer();

        // Utiliser les points du cache au lieu d'une nouvelle requête
        Integer points = playerPointsMap.getOrDefault(player.getId(), 0);
        totalPoints += points;

        playerInfos.add(
            LeaderboardEntryDTO.PlayerInfo.builder()
                .playerId(player.getId())
                .username(player.getUsername())
                .nickname(player.getNickname())
                .region(player.getRegion())
                .tranche(player.getTranche())
                .points(points.longValue())
                .build());
      }

      entries.add(
          LeaderboardEntryDTO.builder()
              .teamId(team.getId())
              .teamName(team.getName())
              .ownerId(team.getOwner().getId())
              .ownerUsername(team.getOwner().getUsername())
              .totalPoints(totalPoints)
              .players(playerInfos)
              .build());
    }

    // 4. Trier par points décroissants et assigner les rangs
    entries.sort((a, b) -> Long.compare(b.getTotalPoints(), a.getTotalPoints()));

    for (int i = 0; i < entries.size(); i++) {
      entries.get(i).setRank(i + 1);
    }

    log.info("[OK] Leaderboard généré avec {} équipes - SANS N+1 queries", entries.size());
    return entries;
  }

  /** Obtenir le leaderboard pour une game spécifique */
  public List<LeaderboardEntryDTO> getLeaderboardByGame(UUID gameId) {
    log.info("[LEADERBOARD] Recuperation du leaderboard pour la game {}", gameId);

    // 1. Récupérer les équipes de cette game avec FETCH EAGER
    List<Team> teams = teamRepository.findByGameIdWithFetch(gameId);
    log.debug("[DATA] {} equipes trouvees pour la game {}", teams.size(), gameId);

    if (teams.isEmpty()) {
      log.warn("[WARN] Aucune équipe trouvée pour la game {}", gameId);
      return new ArrayList<>();
    }

    // 2. Récupérer tous les scores en UNE SEULE requête
    Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(2025);

    // 3. Construire le leaderboard
    List<LeaderboardEntryDTO> entries = new ArrayList<>();

    for (Team team : teams) {
      long totalPoints = 0;
      List<LeaderboardEntryDTO.PlayerInfo> playerInfos = new ArrayList<>();

      for (TeamPlayer teamPlayer : team.getPlayers()) {
        if (!teamPlayer.isActive()) continue;

        Player player = teamPlayer.getPlayer();
        Integer points = playerPointsMap.getOrDefault(player.getId(), 0);
        totalPoints += points;

        playerInfos.add(
            LeaderboardEntryDTO.PlayerInfo.builder()
                .playerId(player.getId())
                .username(player.getUsername())
                .nickname(player.getNickname())
                .region(player.getRegion())
                .tranche(player.getTranche())
                .points(points.longValue())
                .build());
      }

      entries.add(
          LeaderboardEntryDTO.builder()
              .teamId(team.getId())
              .teamName(team.getName())
              .ownerId(team.getOwner().getId())
              .ownerUsername(team.getOwner().getUsername())
              .totalPoints(totalPoints)
              .players(playerInfos)
              .build());
    }

    // 4. Trier par points décroissants et assigner les rangs
    entries.sort((a, b) -> Long.compare(b.getTotalPoints(), a.getTotalPoints()));

    for (int i = 0; i < entries.size(); i++) {
      entries.get(i).setRank(i + 1);
    }

    log.info("[OK] Leaderboard game {} généré avec {} équipes", gameId, entries.size());
    return entries;
  }

  /** Récupère le classement d'une équipe spécifique */
  public LeaderboardEntryDTO getTeamRanking(String teamId) {
    UUID teamUuid = UUID.fromString(teamId);
    Optional<Team> teamOpt = teamRepository.findById(teamUuid);
    if (teamOpt.isEmpty()) {
      throw new RuntimeException("Équipe non trouvée");
    }

    Team team = teamOpt.get();
    List<LeaderboardEntryDTO> allEntries = getLeaderboard(team.getSeason());

    return allEntries.stream()
        .filter(entry -> entry.getTeamId().equals(teamUuid))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Équipe non trouvée dans le classement"));
  }
}
