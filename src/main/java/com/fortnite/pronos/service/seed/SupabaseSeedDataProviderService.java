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
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.service.MockDataGeneratorService.MockDataSet;
import com.fortnite.pronos.service.MockDataGeneratorService.PlayerWithScore;
import com.fortnite.pronos.service.supabase.SupabaseTableService;
import com.fortnite.pronos.service.supabase.dto.SupabasePlayerAssignmentRow;
import com.fortnite.pronos.service.supabase.dto.SupabasePlayerRow;
import com.fortnite.pronos.service.supabase.dto.SupabaseScoreRow;
import com.fortnite.pronos.service.supabase.dto.SupabaseTeamPlayerRow;
import com.fortnite.pronos.service.supabase.dto.SupabaseTeamRow;
import com.fortnite.pronos.service.supabase.dto.SupabaseUserRow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "fortnite.supabase.url", matchIfMissing = false)
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
    List<SupabasePlayerAssignmentRow> assignments = supabaseTableService.fetchPlayerAssignments();
    if (!assignments.isEmpty()) {
      return transformAssignments(assignments);
    }

    return fetchFromPrimaryTables();
  }

  private MockDataSet fetchFromPrimaryTables() {
    if (!supabaseProperties.hasSeedGameId()) {
      log.warn("Supabase assignments empty and no seed game id configured");
      return MockDataSet.empty();
    }

    UUID gameId;
    try {
      gameId = UUID.fromString(supabaseProperties.getSeedGameId());
    } catch (IllegalArgumentException ex) {
      log.warn("Invalid supabase seed game id: {}", supabaseProperties.getSeedGameId());
      return MockDataSet.empty();
    }

    List<SupabaseUserRow> users = supabaseTableService.fetchUsers();
    List<SupabasePlayerRow> players = supabaseTableService.fetchPlayers();
    List<SupabaseScoreRow> scores = supabaseTableService.fetchScores();
    List<SupabaseTeamRow> teams = supabaseTableService.fetchTeams();
    List<SupabaseTeamPlayerRow> teamPlayers = supabaseTableService.fetchTeamPlayers();

    if (teams.isEmpty() || teamPlayers.isEmpty() || players.isEmpty()) {
      log.warn("Supabase tables missing data for seed fallback");
      return MockDataSet.empty();
    }

    Map<UUID, String> usernamesById = mapUsernames(users);
    Map<UUID, Player> playersById = mapPlayers(players);
    Map<UUID, Score> scoresByPlayer = mapScores(scores, playersById);
    Map<UUID, List<SupabaseTeamPlayerRow>> teamPlayersByTeam = mapTeamPlayers(teamPlayers);

    Map<String, List<PlayerWithScore>> playersByPronosticator = new LinkedHashMap<>();

    for (SupabaseTeamRow team : teams) {
      if (team.gameId() == null || !team.gameId().equals(gameId)) {
        continue;
      }
      String pronostiqueur = resolvePronostiqueur(usernamesById, team.ownerId());
      List<SupabaseTeamPlayerRow> roster = teamPlayersByTeam.get(team.id());
      if (roster == null || roster.isEmpty()) {
        continue;
      }

      for (SupabaseTeamPlayerRow row : roster) {
        Player player = playersById.get(row.playerId());
        if (player == null) {
          continue;
        }
        Score score = scoresByPlayer.get(player.getId());
        if (score == null) {
          score = defaultScore(player);
        }

        PlayerWithScore playerWithScore = new PlayerWithScore(pronostiqueur, player, score, 0);
        playersByPronosticator
            .computeIfAbsent(pronostiqueur, k -> new ArrayList<>())
            .add(playerWithScore);
      }
    }

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

  private MockDataSet transformAssignments(List<SupabasePlayerAssignmentRow> rows) {
    Map<String, List<PlayerWithScore>> playersByPronosticator = new LinkedHashMap<>();

    for (SupabasePlayerAssignmentRow row : rows) {
      String pronostiqueur = resolvePronostiqueur(row.pronostiqueur());

      Player player = new Player();
      player.setNickname(row.nickname());
      player.setUsername(generateUsername(row.nickname()));
      player.setRegion(parseRegion(row.region()));
      player.setTranche(DEFAULT_TRANCHE);
      player.setCurrentSeason(CURRENT_SEASON);

      Score score = new Score();
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

  private Map<UUID, String> mapUsernames(List<SupabaseUserRow> rows) {
    Map<UUID, String> usernames = new LinkedHashMap<>();
    for (SupabaseUserRow row : rows) {
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

  private Map<UUID, Player> mapPlayers(List<SupabasePlayerRow> rows) {
    Map<UUID, Player> players = new LinkedHashMap<>();
    for (SupabasePlayerRow row : rows) {
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

      Player player = new Player();
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

  private Map<UUID, Score> mapScores(List<SupabaseScoreRow> rows, Map<UUID, Player> playersById) {
    Map<UUID, Score> scores = new LinkedHashMap<>();
    for (SupabaseScoreRow row : rows) {
      if (row.playerId() == null) {
        continue;
      }
      if (row.season() != null && !row.season().equals(CURRENT_SEASON)) {
        continue;
      }
      Player player = playersById.get(row.playerId());
      if (player == null) {
        continue;
      }
      Score score = buildScore(player, row);
      Score existing = scores.get(row.playerId());
      if (existing == null || score.getPoints() > existing.getPoints()) {
        scores.put(row.playerId(), score);
      }
    }
    return scores;
  }

  private Map<UUID, List<SupabaseTeamPlayerRow>> mapTeamPlayers(List<SupabaseTeamPlayerRow> rows) {
    Map<UUID, List<SupabaseTeamPlayerRow>> mapped = new LinkedHashMap<>();
    for (SupabaseTeamPlayerRow row : rows) {
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

  private Score buildScore(Player player, SupabaseScoreRow row) {
    Score score = new Score();
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

  private Score defaultScore(Player player) {
    Score score = new Score();
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

  private Player.Region parseRegion(String regionStr) {
    if (regionStr == null || regionStr.isBlank()) {
      return Player.Region.EU;
    }
    try {
      return Player.Region.valueOf(regionStr.toUpperCase().trim());
    } catch (IllegalArgumentException e) {
      return Player.Region.EU;
    }
  }

  private String generateUsername(String nickname) {
    if (nickname == null || nickname.isBlank()) {
      return "unknown_user";
    }
    return nickname.toLowerCase().replaceAll("[^a-z0-9]", "_");
  }
}
