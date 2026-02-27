package com.fortnite.pronos.service.leaderboard;

import java.util.*;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for leaderboard debug operations.
 *
 * <p>Extracted from LeaderboardService to respect SRP (Single Responsibility Principle) and
 * CLAUDE.md 500-line limit.
 *
 * <p>This service is only active in development/test profiles to avoid pollution in production.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Profile({"dev", "test"})
@Transactional(readOnly = true)
public class LeaderboardDebugService {

  private static final String SEASON_KEY = "season";
  private static final int DEBUG_SAMPLE_SIZE = 3;

  private final com.fortnite.pronos.repository.TeamRepository teamRepository;
  private final com.fortnite.pronos.repository.PlayerRepository playerRepository;
  private final com.fortnite.pronos.repository.ScoreRepository scoreRepository;

  /** Obtenir les statistiques de debug pour une saison */
  public Map<String, Object> getDebugStats(int season) {
    Map<String, Object> debug = new HashMap<>();

    long teamCount = teamRepository.findBySeason(season).size();
    debug.put("totalTeams", teamCount);
    debug.put(SEASON_KEY, season);

    long playerCount = playerRepository.count();
    debug.put("totalPlayers", playerCount);

    long scoreCount = scoreRepository.count();
    debug.put("totalScores", scoreCount);

    List<Object[]> rawScores = scoreRepository.findAllBySeasonGroupedByPlayerRaw(season);
    debug.put("rawScoresCount", rawScores.size());
    debug.put(
        "rawScoresSample",
        rawScores.stream()
            .limit(DEBUG_SAMPLE_SIZE)
            .map(row -> Map.of("playerId", row[0], "totalPoints", row[1]))
            .toList());

    Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(season);
    debug.put("playerPointsMapSize", playerPointsMap.size());
    debug.put(
        "totalPointsFromMap", playerPointsMap.values().stream().mapToInt(Integer::intValue).sum());

    return debug;
  }

  /** Obtenir des statistiques de debug simplifiÃ©es */
  public Map<String, Object> getDebugSimple() {
    Map<String, Object> debug = new HashMap<>();

    debug.put("totalPlayers", playerRepository.count());
    debug.put("totalTeams", teamRepository.count());
    debug.put("totalScores", scoreRepository.count());

    debug.put(
        "playersSample",
        playerRepository.findAll().stream()
            .limit(DEBUG_SAMPLE_SIZE)
            .map(
                player ->
                    Map.of(
                        "id",
                        player.getId(),
                        "nickname",
                        player.getNickname(),
                        "region",
                        player.getRegion().name(),
                        SEASON_KEY,
                        player.getCurrentSeason()))
            .toList());

    debug.put(
        "scoresSample",
        scoreRepository.findAll().stream()
            .limit(DEBUG_SAMPLE_SIZE)
            .map(
                score ->
                    Map.of(
                        "playerNickname",
                        score.getPlayer().getNickname(),
                        SEASON_KEY,
                        score.getSeason(),
                        "points",
                        score.getPoints()))
            .toList());

    return debug;
  }
}
