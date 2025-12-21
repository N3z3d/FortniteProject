package com.fortnite.pronos.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.ScoreRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service for loading player data from the CSV file with pronosticator assignments. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CsvDataLoaderService {

  private static final String CSV_RESOURCE_PATH = "data/fortnite_data.csv";
  private static final String REQUIRED_HEADER_TOKEN = "Pronostiqueur";
  private static final int EXPECTED_COLUMNS = 6;
  private static final int CURRENT_SEASON = 2025;

  private final PlayerRepository playerRepository;
  private final ScoreRepository scoreRepository;

  // Map to store player assignments by pronosticator.
  private final Map<String, List<Player>> playersByPronosticator = new HashMap<>();

  /** Load all players and scores from the CSV file. */
  @Transactional
  public void loadAllCsvData() {
    log.info("CSV load start: path={}, season={}", CSV_RESOURCE_PATH, CURRENT_SEASON);
    playersByPronosticator.clear();

    CsvParseResult parseResult = parseCsvRows();
    if (!parseResult.hasRows()) {
      log.warn(
          "CSV load end: status=failed, reason={}, errors={}",
          parseResult.failureReason(),
          parseResult.errorCount());
      return;
    }

    CsvSaveResult saveResult = saveCsvRows(parseResult.rows());
    log.info(
        "CSV load end: status=success, rows={}, newPlayers={}, updatedPlayers={}, newScores={}, updatedScores={}, assignments={}",
        parseResult.rows().size(),
        saveResult.newPlayers(),
        saveResult.updatedPlayers(),
        saveResult.newScores(),
        saveResult.updatedScores(),
        playersByPronosticator.size());
  }

  /** Return players assigned to a specific pronosticator. */
  public List<Player> getPlayersByPronosticator(String pronostiqueur) {
    return playersByPronosticator.getOrDefault(pronostiqueur, new ArrayList<>());
  }

  /** Return all available pronosticators. */
  public List<String> getAllPronosticators() {
    return new ArrayList<>(playersByPronosticator.keySet());
  }

  /** Return a copy of all assignments. */
  public Map<String, List<Player>> getAllPlayerAssignments() {
    return new HashMap<>(playersByPronosticator);
  }

  /** Clear in-memory assignments (useful for tests). */
  public void clearAssignments() {
    playersByPronosticator.clear();
  }

  private CsvParseResult parseCsvRows() {
    ClassPathResource resource = new ClassPathResource(CSV_RESOURCE_PATH);
    if (!resource.exists()) {
      log.warn("CSV parse failed: reason=file_not_found, path={}", CSV_RESOURCE_PATH);
      return CsvParseResult.failure("file_not_found");
    }

    List<CsvRow> rows = new ArrayList<>();
    int errorCount = 0;

    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

      String header = reader.readLine();
      if (header == null || !header.contains(REQUIRED_HEADER_TOKEN)) {
        log.warn("CSV parse failed: reason=invalid_header, header={}", header);
        return CsvParseResult.failure("invalid_header");
      }

      String line;
      int lineNumber = 1;
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        if (line.trim().isEmpty()) {
          continue;
        }

        CsvRow row = parseCsvLine(line, lineNumber);
        if (row == null) {
          errorCount++;
          continue;
        }
        rows.add(row);
      }
    } catch (Exception e) {
      log.error("CSV parse failed: reason=io_error", e);
      return CsvParseResult.failure("io_error");
    }

    if (rows.isEmpty()) {
      log.warn("CSV parse failed: reason=no_rows");
      return CsvParseResult.failure("no_rows");
    }

    log.info("CSV parse ok: rows={}, errors={}", rows.size(), errorCount);
    return CsvParseResult.success(rows, errorCount);
  }

  private CsvRow parseCsvLine(String line, int lineNumber) {
    String[] parts = line.split(",");
    if (parts.length < EXPECTED_COLUMNS) {
      log.warn("CSV parse failed: line={}, reason=invalid_columns, value={}", lineNumber, line);
      return null;
    }

    String pronostiqueur = cleanField(parts[0]);
    String nickname = cleanField(parts[1]);
    String region = cleanField(parts[2]);
    String pointsStr = cleanField(parts[3]);
    String classementStr = cleanField(parts[4]);
    String tranche = cleanField(parts[5]);

    if (pronostiqueur.isEmpty() || nickname.isEmpty() || region.isEmpty()) {
      log.warn("CSV parse failed: line={}, reason=missing_required, value={}", lineNumber, line);
      return null;
    }

    int points = parseIntSafely(pointsStr, 0);
    int classement = parseIntSafely(classementStr, 0);
    Player.Region playerRegion = parseRegion(region);

    String resolvedTranche = tranche.isEmpty() ? "1-5" : tranche;
    return new CsvRow(pronostiqueur, nickname, playerRegion, points, classement, resolvedTranche);
  }

  private CsvSaveResult saveCsvRows(List<CsvRow> rows) {
    Map<String, Player> playersByNickname = loadPlayersByNickname();
    List<Player> newPlayers = new ArrayList<>();
    int updatedPlayers = 0;

    for (CsvRow row : rows) {
      String key = normalizeNickname(row.nickname());
      Player player = playersByNickname.get(key);
      if (player == null) {
        player = buildPlayer(row);
        newPlayers.add(player);
        playersByNickname.put(key, player);
      } else if (updatePlayerFromRow(player, row)) {
        updatedPlayers++;
      }

      playersByPronosticator
          .computeIfAbsent(row.pronostiqueur(), keyValue -> new ArrayList<>())
          .add(player);
    }

    if (!newPlayers.isEmpty()) {
      playerRepository.saveAll(newPlayers);
    }

    Map<UUID, Score> scoresByPlayerId = loadScoresByPlayerId();
    List<Score> newScores = new ArrayList<>();
    int updatedScores = 0;

    for (CsvRow row : rows) {
      Player player = playersByNickname.get(normalizeNickname(row.nickname()));
      if (player == null || player.getId() == null) {
        continue;
      }

      Score score = scoresByPlayerId.get(player.getId());
      if (score == null) {
        newScores.add(buildScore(player, row));
      } else if (score.getPoints() == null || score.getPoints() != row.points()) {
        score.setPoints(row.points());
        updatedScores++;
      }
    }

    if (!newScores.isEmpty()) {
      scoreRepository.saveAll(newScores);
    }

    return new CsvSaveResult(newPlayers.size(), updatedPlayers, newScores.size(), updatedScores);
  }

  private Map<String, Player> loadPlayersByNickname() {
    Map<String, Player> result = new HashMap<>();
    for (Player player : playerRepository.findAll()) {
      result.putIfAbsent(normalizeNickname(player.getNickname()), player);
    }
    return result;
  }

  private Map<UUID, Score> loadScoresByPlayerId() {
    Map<UUID, Score> result = new HashMap<>();
    for (Score score : scoreRepository.findBySeason(CURRENT_SEASON)) {
      if (score.getPlayer() != null && score.getPlayer().getId() != null) {
        result.putIfAbsent(score.getPlayer().getId(), score);
      }
    }
    return result;
  }

  private Player buildPlayer(CsvRow row) {
    String cleanUsername = buildUsername(row.nickname());
    return Player.builder()
        .username(cleanUsername)
        .nickname(row.nickname())
        .region(row.region())
        .tranche(row.tranche())
        .currentSeason(CURRENT_SEASON)
        .build();
  }

  private boolean updatePlayerFromRow(Player player, CsvRow row) {
    boolean updated = false;
    if (player.getRegion() != row.region()) {
      player.setRegion(row.region());
      updated = true;
    }
    if (player.getTranche() == null || !player.getTranche().equals(row.tranche())) {
      player.setTranche(row.tranche());
      updated = true;
    }
    if (player.getCurrentSeason() == null || !player.getCurrentSeason().equals(CURRENT_SEASON)) {
      player.setCurrentSeason(CURRENT_SEASON);
      updated = true;
    }
    return updated;
  }

  private Score buildScore(Player player, CsvRow row) {
    Score score = new Score();
    score.setPlayer(player);
    score.setSeason(CURRENT_SEASON);
    score.setPoints(row.points());
    score.setDate(LocalDate.now());
    score.setTimestamp(OffsetDateTime.now());
    return score;
  }

  private String normalizeNickname(String nickname) {
    return nickname == null ? "" : nickname.trim().toLowerCase();
  }

  private String cleanField(String field) {
    return field == null ? "" : field.trim().replace("\"", "");
  }

  private int parseIntSafely(String value, int defaultValue) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private Player.Region parseRegion(String region) {
    try {
      return Player.Region.valueOf(region.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("CSV parse: invalid region '{}', defaulting to EU", region);
      return Player.Region.EU;
    }
  }

  private String buildUsername(String nickname) {
    String cleanUsername = nickname.toLowerCase().replaceAll("[^a-z0-9]", "");
    if (cleanUsername.isEmpty()) {
      cleanUsername = "player" + Math.abs(nickname.hashCode());
    }
    return cleanUsername;
  }

  private record CsvRow(
      String pronostiqueur,
      String nickname,
      Player.Region region,
      int points,
      int classement,
      String tranche) {}

  private record CsvParseResult(List<CsvRow> rows, int errorCount, String failureReason) {
    static CsvParseResult success(List<CsvRow> rows, int errorCount) {
      return new CsvParseResult(rows, errorCount, null);
    }

    static CsvParseResult failure(String reason) {
      return new CsvParseResult(List.of(), 0, reason);
    }

    boolean hasRows() {
      return rows != null && !rows.isEmpty();
    }
  }

  private record CsvSaveResult(
      int newPlayers, int updatedPlayers, int newScores, int updatedScores) {}
}
