package com.fortnite.pronos.service.leaderboard;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.model.*;
import com.fortnite.pronos.repository.*;

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

  private final TeamRepository teamRepository;
  private final PlayerRepository playerRepository;
  private final ScoreRepository scoreRepository;

  /** Obtenir les statistiques de debug pour une saison */
  public Map<String, Object> getDebugStats(int season) {
    Map<String, Object> debug = new HashMap<>();

    long teamCount = teamRepository.findBySeason(season).size();
    debug.put("totalTeams", teamCount);
    debug.put("season", season);

    long playerCount = playerRepository.count();
    debug.put("totalPlayers", playerCount);

    long scoreCount = scoreRepository.count();
    debug.put("totalScores", scoreCount);

    List<Object[]> rawScores = scoreRepository.findAllBySeasonGroupedByPlayerRaw(season);
    debug.put("rawScoresCount", rawScores.size());
    debug.put(
        "rawScoresSample",
        rawScores.stream()
            .limit(3)
            .map(row -> Map.of("playerId", row[0], "totalPoints", row[1]))
            .collect(Collectors.toList()));

    Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(season);
    debug.put("playerPointsMapSize", playerPointsMap.size());
    debug.put(
        "totalPointsFromMap", playerPointsMap.values().stream().mapToInt(Integer::intValue).sum());

    return debug;
  }

  /** Obtenir des statistiques de debug simplifiées */
  public Map<String, Object> getDebugSimple() {
    Map<String, Object> debug = new HashMap<>();

    debug.put("totalPlayers", playerRepository.count());
    debug.put("totalTeams", teamRepository.count());
    debug.put("totalScores", scoreRepository.count());

    debug.put(
        "playersSample",
        playerRepository.findAll().stream()
            .limit(3)
            .map(
                player ->
                    Map.of(
                        "id",
                        player.getId(),
                        "nickname",
                        player.getNickname(),
                        "region",
                        player.getRegion().name(),
                        "season",
                        player.getCurrentSeason()))
            .collect(Collectors.toList()));

    debug.put(
        "scoresSample",
        scoreRepository.findAll().stream()
            .limit(3)
            .map(
                score ->
                    Map.of(
                        "playerNickname",
                        score.getPlayer().getNickname(),
                        "season",
                        score.getSeason(),
                        "points",
                        score.getPoints()))
            .collect(Collectors.toList()));

    return debug;
  }
}
