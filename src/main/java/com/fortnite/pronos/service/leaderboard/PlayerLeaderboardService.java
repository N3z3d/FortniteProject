package com.fortnite.pronos.service.leaderboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.dto.PlayerLeaderboardEntryDTO;
import com.fortnite.pronos.dto.leaderboard.TeamInfoDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@SuppressWarnings({"java:S112"})
public class PlayerLeaderboardService {

  private static final int DEFAULT_GAME_LEADERBOARD_SEASON = 2025;
  private static final int POINTS_PER_GAME_BUCKET = 1000;
  private static final int MINIMUM_GAMES_DIVISOR = 1;
  private static final double ZERO_AVERAGE_POINTS = 0.0;
  private static final int FIRST_RANK = 1;

  private final com.fortnite.pronos.repository.PlayerRepository playerRepository;
  private final com.fortnite.pronos.repository.ScoreRepository scoreRepository;
  private final com.fortnite.pronos.repository.TeamRepository teamRepository;

  @Cacheable(value = "playerScores", key = "'players_' + #season")
  public List<PlayerLeaderboardEntryDTO> getPlayerLeaderboard(int season) {
    log.info("[PLAYER] Retrieving player leaderboard - season={}", season);
    try {
      List<com.fortnite.pronos.model.Player> players = playerRepository.findAll();
      log.info("[DATA] {} players found", players.size());

      Map<UUID, Integer> playerPointsMap = scoreRepository.findAllBySeasonGroupedByPlayer(season);
      log.info("[DATA] {} score entries found for season {}", playerPointsMap.size(), season);

      List<com.fortnite.pronos.model.Team> teams = teamRepository.findBySeasonWithFetch(season);
      TeamOwnershipContext context = buildTeamOwnershipContext(teams);

      List<PlayerLeaderboardEntryDTO> entries =
          buildLeaderboardEntries(players, playerPointsMap, context);
      sortAndAssignRanks(entries);

      log.info("[OK] Player leaderboard generated with {} players", entries.size());
      return entries;
    } catch (Exception exception) {
      log.error("[ERROR] Failed to generate player leaderboard", exception);
      throw new RuntimeException("Failed to generate player leaderboard", exception);
    }
  }

  public List<PlayerLeaderboardEntryDTO> getPlayerLeaderboardByGame(UUID gameId) {
    log.info("[PLAYER] Retrieving player leaderboard for game={}", gameId);
    try {
      List<com.fortnite.pronos.model.Team> teams = teamRepository.findByGameIdWithFetch(gameId);
      log.info("[DATA] {} teams found for game {}", teams.size(), gameId);
      if (teams.isEmpty()) {
        log.warn("[WARN] No teams found for game {}", gameId);
        return new ArrayList<>();
      }

      TeamOwnershipContext context = buildTeamOwnershipContext(teams);
      log.info("[DATA] {} unique players found in teams", context.playerIds().size());

      List<com.fortnite.pronos.model.Player> players =
          playerRepository.findAllById(context.playerIds());
      Map<UUID, Integer> playerPointsMap =
          scoreRepository.findAllBySeasonGroupedByPlayer(DEFAULT_GAME_LEADERBOARD_SEASON);

      List<PlayerLeaderboardEntryDTO> entries =
          buildLeaderboardEntries(players, playerPointsMap, context);
      sortAndAssignRanks(entries);

      log.info(
          "[OK] Game player leaderboard generated for game {} with {} players",
          gameId,
          entries.size());
      return entries;
    } catch (Exception exception) {
      log.error(
          "[ERROR] Failed to generate game player leaderboard for game {}", gameId, exception);
      throw new RuntimeException("Failed to generate player leaderboard", exception);
    }
  }

  private TeamOwnershipContext buildTeamOwnershipContext(
      List<com.fortnite.pronos.model.Team> teams) {
    Set<UUID> playerIds = new HashSet<>();
    Map<UUID, List<TeamInfoDto>> playerTeamsMap = new HashMap<>();
    Map<UUID, List<String>> playerPronostiqueurMap = new HashMap<>();

    for (com.fortnite.pronos.model.Team team : teams) {
      addTeamPlayersContext(team, playerIds, playerTeamsMap, playerPronostiqueurMap);
    }
    return new TeamOwnershipContext(playerIds, playerTeamsMap, playerPronostiqueurMap);
  }

  private void addTeamPlayersContext(
      com.fortnite.pronos.model.Team team,
      Set<UUID> playerIds,
      Map<UUID, List<TeamInfoDto>> playerTeamsMap,
      Map<UUID, List<String>> playerPronostiqueurMap) {
    for (com.fortnite.pronos.model.TeamPlayer teamPlayer : team.getPlayers()) {
      if (!teamPlayer.isActive()) {
        continue;
      }
      UUID playerId = teamPlayer.getPlayer().getId();
      playerIds.add(playerId);

      playerTeamsMap
          .computeIfAbsent(playerId, key -> new ArrayList<>())
          .add(new TeamInfoDto(team.getId().toString(), team.getName(), team.getOwnerUsername()));

      playerPronostiqueurMap
          .computeIfAbsent(playerId, key -> new ArrayList<>())
          .add(team.getOwnerUsername());
    }
  }

  private List<PlayerLeaderboardEntryDTO> buildLeaderboardEntries(
      List<com.fortnite.pronos.model.Player> players,
      Map<UUID, Integer> playerPointsMap,
      TeamOwnershipContext context) {
    List<PlayerLeaderboardEntryDTO> entries = new ArrayList<>();
    for (com.fortnite.pronos.model.Player player : players) {
      entries.add(buildLeaderboardEntry(player, playerPointsMap, context));
    }
    return entries;
  }

  private PlayerLeaderboardEntryDTO buildLeaderboardEntry(
      com.fortnite.pronos.model.Player player,
      Map<UUID, Integer> playerPointsMap,
      TeamOwnershipContext context) {
    UUID playerId = player.getId();
    int totalPoints = playerPointsMap.getOrDefault(playerId, 0);
    List<TeamInfoDto> playerTeams =
        context.playerTeamsMap().getOrDefault(playerId, new ArrayList<>());
    List<String> playerPronostiqueurs =
        context.playerPronostiqueurMap().getOrDefault(playerId, new ArrayList<>());

    PlayerLeaderboardEntryDTO entry = new PlayerLeaderboardEntryDTO();
    entry.setPlayerId(playerId.toString());
    entry.setNickname(player.getNickname());
    entry.setUsername(player.getUsername());
    entry.setRegion(player.getRegion());
    entry.setTranche(player.getTranche());
    entry.setTotalPoints(totalPoints);
    entry.setAvgPointsPerGame(calculateAveragePointsPerGame(totalPoints));
    entry.setBestScore(totalPoints);
    entry.setTeamsCount(playerTeams.size());
    entry.setTeams(playerTeams);
    entry.setPronostiqueurs(playerPronostiqueurs.stream().distinct().toList());
    return entry;
  }

  private double calculateAveragePointsPerGame(int totalPoints) {
    if (totalPoints <= 0) {
      return ZERO_AVERAGE_POINTS;
    }
    int estimatedGamesPlayed =
        Math.max(MINIMUM_GAMES_DIVISOR, totalPoints / POINTS_PER_GAME_BUCKET);
    return (double) totalPoints / estimatedGamesPlayed;
  }

  private void sortAndAssignRanks(List<PlayerLeaderboardEntryDTO> entries) {
    entries.sort((left, right) -> Integer.compare(right.getTotalPoints(), left.getTotalPoints()));
    for (int index = 0; index < entries.size(); index++) {
      entries.get(index).setRank(index + FIRST_RANK);
    }
  }

  private record TeamOwnershipContext(
      Set<UUID> playerIds,
      Map<UUID, List<TeamInfoDto>> playerTeamsMap,
      Map<UUID, List<String>> playerPronostiqueurMap) {}
}
