package com.fortnite.pronos.service.seed;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import com.fortnite.pronos.config.SupabaseProperties;
import com.fortnite.pronos.service.MockDataGeneratorService.MockDataSet;
import com.fortnite.pronos.service.MockDataGeneratorService.PlayerWithScore;
import com.fortnite.pronos.service.supabase.SupabaseTableService;
import com.fortnite.pronos.service.supabase.dto.SupabasePlayerAssignmentRowDto;
import com.fortnite.pronos.service.supabase.dto.SupabasePlayerRowDto;
import com.fortnite.pronos.service.supabase.dto.SupabaseScoreRowDto;
import com.fortnite.pronos.service.supabase.dto.SupabaseTeamPlayerRowDto;
import com.fortnite.pronos.service.supabase.dto.SupabaseTeamRowDto;
import com.fortnite.pronos.service.supabase.dto.SupabaseUserRowDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "fortnite.supabase.url", matchIfMissing = false)
@SuppressWarnings({"java:S135"})
public class SupabaseSeedDataProviderService implements SeedDataProvider {

  private static final String KEY = "supabase";
  private static final int CURRENT_SEASON = 2025;
  private static final String DEFAULT_TRANCHE = "1-5";

  private final SupabaseProperties supabaseProperties;
  private final SupabaseTableService supabaseTableService;

  @Override
  public String getKey() {
    return KEY;
  }

  @Override
  public MockDataSet loadSeedData() {
    if (!supabaseProperties.isConfigured()) {
      log.warn("Supabase not configured, returning empty data set");
      return MockDataSet.empty();
    }

    try {
      return fetchFromSupabase();
    } catch (RestClientException e) {
      log.error("Failed to fetch data from Supabase: {}", e.getMessage());
      return MockDataSet.empty();
    }
  }

  private MockDataSet fetchFromSupabase() {
    List<SupabasePlayerAssignmentRowDto> assignments =
        supabaseTableService.fetchPlayerAssignments();
    if (!assignments.isEmpty()) {
      return transformAssignments(assignments);
    }

    return fetchFromPrimaryTables();
  }

  private MockDataSet fetchFromPrimaryTables() {
    UUID gameId = resolveSeedGameId();
    if (gameId == null) {
      return MockDataSet.empty();
    }

    PrimaryTables tables = loadPrimaryTables();
    if (tables.hasMissingRequiredData()) {
      log.warn("Supabase tables missing data for seed fallback");
      return MockDataSet.empty();
    }

    Map<UUID, String> usernamesById = mapUsernames(tables.users());
    Map<UUID, com.fortnite.pronos.model.Player> playersById = mapPlayers(tables.players());
    Map<UUID, com.fortnite.pronos.model.Score> scoresByPlayer =
        mapScores(tables.scores(), playersById);
    Map<UUID, List<SupabaseTeamPlayerRowDto>> teamPlayersByTeam =
        mapTeamPlayers(tables.teamPlayers());

    Map<String, List<PlayerWithScore>> playersByPronosticator =
        buildPronosticatorFallbackData(
            gameId, tables.teams(), usernamesById, playersById, scoresByPlayer, teamPlayersByTeam);

    return toMockDataSet(playersByPronosticator, gameId);
  }

  private UUID resolveSeedGameId() {
    if (!supabaseProperties.hasSeedGameId()) {
      log.warn("Supabase assignments empty and no seed game id configured");
      return null;
    }
    try {
      return UUID.fromString(supabaseProperties.getSeedGameId());
    } catch (IllegalArgumentException ex) {
      log.warn("Invalid supabase seed game id: {}", supabaseProperties.getSeedGameId());
      return null;
    }
  }

  private PrimaryTables loadPrimaryTables() {
    return new PrimaryTables(
        supabaseTableService.fetchUsers(),
        supabaseTableService.fetchPlayers(),
        supabaseTableService.fetchScores(),
        supabaseTableService.fetchTeams(),
        supabaseTableService.fetchTeamPlayers());
  }

  private Map<String, List<PlayerWithScore>> buildPronosticatorFallbackData(
      UUID gameId,
      List<SupabaseTeamRowDto> teams,
      Map<UUID, String> usernamesById,
      Map<UUID, com.fortnite.pronos.model.Player> playersById,
      Map<UUID, com.fortnite.pronos.model.Score> scoresByPlayer,
      Map<UUID, List<SupabaseTeamPlayerRowDto>> teamPlayersByTeam) {
    Map<String, List<PlayerWithScore>> playersByPronosticator = new LinkedHashMap<>();
    for (SupabaseTeamRowDto team : teams) {
      if (!belongsToGame(team, gameId)) {
        continue;
      }
      addTeamRosterToPronosticatorMap(
          playersByPronosticator,
          resolvePronostiqueur(usernamesById, team.ownerId()),
          team.id(),
          playersById,
          scoresByPlayer,
          teamPlayersByTeam);
    }
    return playersByPronosticator;
  }

  private boolean belongsToGame(SupabaseTeamRowDto team, UUID gameId) {
    return team.gameId() != null && team.gameId().equals(gameId);
  }

  private void addTeamRosterToPronosticatorMap(
      Map<String, List<PlayerWithScore>> playersByPronosticator,
      String pronostiqueur,
      UUID teamId,
      Map<UUID, com.fortnite.pronos.model.Player> playersById,
      Map<UUID, com.fortnite.pronos.model.Score> scoresByPlayer,
      Map<UUID, List<SupabaseTeamPlayerRowDto>> teamPlayersByTeam) {
    List<SupabaseTeamPlayerRowDto> roster = teamPlayersByTeam.get(teamId);
    if (roster == null || roster.isEmpty()) {
      return;
    }
    for (SupabaseTeamPlayerRowDto row : roster) {
      com.fortnite.pronos.model.Player player = playersById.get(row.playerId());
      if (player == null) {
        continue;
      }
      com.fortnite.pronos.model.Score score =
          scoresByPlayer.getOrDefault(player.getId(), defaultScore(player));
      PlayerWithScore playerWithScore = new PlayerWithScore(pronostiqueur, player, score, 0);
      playersByPronosticator
          .computeIfAbsent(pronostiqueur, k -> new ArrayList<>())
          .add(playerWithScore);
    }
  }

  private MockDataSet toMockDataSet(
      Map<String, List<PlayerWithScore>> playersByPronosticator, UUID gameId) {
    int total = playersByPronosticator.values().stream().mapToInt(List::size).sum();
    if (total == 0) {
      log.warn("Supabase fallback returned no assignments for game {}", gameId);
      return MockDataSet.empty();
    }
    log.info(
        "Loaded {} players from Supabase fallback for {} pronostiqueurs",
        total,
        playersByPronosticator.size());
    return new MockDataSet(playersByPronosticator, total);
  }

  private MockDataSet transformAssignments(List<SupabasePlayerAssignmentRowDto> rows) {
    Map<String, List<PlayerWithScore>> playersByPronosticator = new LinkedHashMap<>();

    for (SupabasePlayerAssignmentRowDto row : rows) {
      String pronostiqueur = resolvePronostiqueur(row.pronostiqueur());

      com.fortnite.pronos.model.Player player = new com.fortnite.pronos.model.Player();
      player.setNickname(row.nickname());
      player.setUsername(generateUsername(row.nickname()));
      player.setRegion(parseRegion(row.region()));
      player.setTranche(DEFAULT_TRANCHE);
      player.setCurrentSeason(CURRENT_SEASON);

      com.fortnite.pronos.model.Score score = new com.fortnite.pronos.model.Score();
      score.setPlayer(player);
      score.setSeason(CURRENT_SEASON);
      score.setPoints(row.score() != null ? row.score() : 0);

      int classement = row.rank() != null ? row.rank() : 0;
      PlayerWithScore playerWithScore =
          new PlayerWithScore(pronostiqueur, player, score, classement);

      playersByPronosticator
          .computeIfAbsent(pronostiqueur, k -> new ArrayList<>())
          .add(playerWithScore);
    }

    int total = playersByPronosticator.values().stream().mapToInt(List::size).sum();
    log.info(
        "Loaded {} players from Supabase for {} pronostiqueurs",
        total,
        playersByPronosticator.size());

    return new MockDataSet(playersByPronosticator, total);
  }

  private Map<UUID, String> mapUsernames(List<SupabaseUserRowDto> rows) {
    Map<UUID, String> usernames = new LinkedHashMap<>();
    for (SupabaseUserRowDto row : rows) {
      if (row.id() == null) {
        continue;
      }
      String username = row.username();
      if (username == null || username.isBlank()) {
        continue;
      }
      usernames.put(row.id(), username.trim());
    }
    return usernames;
  }

  private Map<UUID, com.fortnite.pronos.model.Player> mapPlayers(List<SupabasePlayerRowDto> rows) {
    Map<UUID, com.fortnite.pronos.model.Player> players = new LinkedHashMap<>();
    for (SupabasePlayerRowDto row : rows) {
      if (row.id() == null) {
        continue;
      }
      String nickname = row.nickname();
      String username = row.username();
      if (username == null || username.isBlank()) {
        username = generateUsername(nickname);
      }
      if (nickname == null || nickname.isBlank()) {
        nickname = username;
      }

      com.fortnite.pronos.model.Player player = new com.fortnite.pronos.model.Player();
      player.setId(row.id());
      player.setFortniteId(row.fortniteId());
      player.setUsername(username);
      player.setNickname(nickname);
      player.setRegion(parseRegion(row.region()));
      player.setTranche(
          row.tranche() != null && !row.tranche().isBlank() ? row.tranche() : DEFAULT_TRANCHE);
      player.setCurrentSeason(row.currentSeason() != null ? row.currentSeason() : CURRENT_SEASON);

      players.put(row.id(), player);
    }
    return players;
  }

  private Map<UUID, com.fortnite.pronos.model.Score> mapScores(
      List<SupabaseScoreRowDto> rows, Map<UUID, com.fortnite.pronos.model.Player> playersById) {
    Map<UUID, com.fortnite.pronos.model.Score> scores = new LinkedHashMap<>();
    for (SupabaseScoreRowDto row : rows) {
      if (row.playerId() == null) {
        continue;
      }
      if (row.season() != null && !row.season().equals(CURRENT_SEASON)) {
        continue;
      }
      com.fortnite.pronos.model.Player player = playersById.get(row.playerId());
      if (player == null) {
        continue;
      }
      com.fortnite.pronos.model.Score score = buildScore(player, row);
      com.fortnite.pronos.model.Score existing = scores.get(row.playerId());
      if (existing == null || score.getPoints() > existing.getPoints()) {
        scores.put(row.playerId(), score);
      }
    }
    return scores;
  }

  private Map<UUID, List<SupabaseTeamPlayerRowDto>> mapTeamPlayers(
      List<SupabaseTeamPlayerRowDto> rows) {
    Map<UUID, List<SupabaseTeamPlayerRowDto>> mapped = new LinkedHashMap<>();
    for (SupabaseTeamPlayerRowDto row : rows) {
      if (row.teamId() == null || row.playerId() == null) {
        continue;
      }
      if (row.until() != null) {
        continue;
      }
      mapped.computeIfAbsent(row.teamId(), k -> new ArrayList<>()).add(row);
    }
    return mapped;
  }

  private com.fortnite.pronos.model.Score buildScore(
      com.fortnite.pronos.model.Player player, SupabaseScoreRowDto row) {
    com.fortnite.pronos.model.Score score = new com.fortnite.pronos.model.Score();
    score.setPlayer(player);
    score.setSeason(row.season() != null ? row.season() : CURRENT_SEASON);
    score.setPoints(row.points() != null ? row.points() : 0);
    if (row.date() != null) {
      score.setDate(row.date());
    }
    if (row.timestamp() != null) {
      score.setTimestamp(row.timestamp());
    }
    return score;
  }

  private com.fortnite.pronos.model.Score defaultScore(com.fortnite.pronos.model.Player player) {
    com.fortnite.pronos.model.Score score = new com.fortnite.pronos.model.Score();
    score.setPlayer(player);
    score.setSeason(CURRENT_SEASON);
    score.setPoints(0);
    score.setDate(LocalDate.now());
    score.setTimestamp(OffsetDateTime.now());
    return score;
  }

  private String resolvePronostiqueur(Map<UUID, String> usernamesById, UUID ownerId) {
    if (ownerId == null) {
      return "Unknown";
    }
    String username = usernamesById.get(ownerId);
    return resolvePronostiqueur(username);
  }

  private String resolvePronostiqueur(String pronostiqueur) {
    if (pronostiqueur == null || pronostiqueur.isBlank()) {
      return "Unknown";
    }
    return pronostiqueur.trim();
  }

  private com.fortnite.pronos.model.Player.Region parseRegion(String regionStr) {
    if (regionStr == null || regionStr.isBlank()) {
      return com.fortnite.pronos.model.Player.Region.EU;
    }
    try {
      return com.fortnite.pronos.model.Player.Region.valueOf(regionStr.toUpperCase().trim());
    } catch (IllegalArgumentException e) {
      return com.fortnite.pronos.model.Player.Region.EU;
    }
  }

  private String generateUsername(String nickname) {
    if (nickname == null || nickname.isBlank()) {
      return "unknown_user";
    }
    return nickname.toLowerCase().replaceAll("[^a-z0-9]", "_");
  }

  private record PrimaryTables(
      List<SupabaseUserRowDto> users,
      List<SupabasePlayerRowDto> players,
      List<SupabaseScoreRowDto> scores,
      List<SupabaseTeamRowDto> teams,
      List<SupabaseTeamPlayerRowDto> teamPlayers) {
    boolean hasMissingRequiredData() {
      return teams.isEmpty() || teamPlayers.isEmpty() || players.isEmpty();
    }
  }
}
