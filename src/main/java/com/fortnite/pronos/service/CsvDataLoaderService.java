package com.fortnite.pronos.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service for loading player data from CSV with pronosticator assignments. */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"java:S135", "java:S2676", "java:S3824"})
public class CsvDataLoaderService {

  private static final String CSV_RESOURCE_PATH = "data/fortnite_data.csv";
  private static final String REQUIRED_HEADER_TOKEN = "Pronostiqueur";
  private static final int EXPECTED_COLUMNS = 6;
  private static final int CURRENT_SEASON = 2025;

  private static final int COLUMN_PRONOSTIQUEUR_INDEX = 0;
  private static final int COLUMN_NICKNAME_INDEX = 1;
  private static final int COLUMN_REGION_INDEX = 2;
  private static final int COLUMN_POINTS_INDEX = 3;
  private static final int COLUMN_RANKING_INDEX = 4;
  private static final int COLUMN_TRANCHE_INDEX = 5;

  private static final int DEFAULT_NUMERIC_VALUE = 0;
  private static final String DEFAULT_TRANCHE = "1-5";
  private static final String EMPTY_STRING = "";
  private static final String USERNAME_FALLBACK_PREFIX = "player";
  private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9]");

  private final com.fortnite.pronos.repository.PlayerRepository playerRepository;
  private final com.fortnite.pronos.repository.ScoreRepository scoreRepository;

  // Map to store player assignments by pronosticator.
  private final Map<String, List<com.fortnite.pronos.model.Player>> playersByPronosticator =
      new HashMap<>();

  /** Load all players and scores from CSV. */
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
  public List<com.fortnite.pronos.model.Player> getPlayersByPronosticator(String pronostiqueur) {
    return playersByPronosticator.getOrDefault(pronostiqueur, new ArrayList<>());
  }

  /** Return all available pronosticators. */
  public List<String> getAllPronosticators() {
    return new ArrayList<>(playersByPronosticator.keySet());
  }

  /** Return a copy of all assignments. */
  public Map<String, List<com.fortnite.pronos.model.Player>> getAllPlayerAssignments() {
    return new HashMap<>(playersByPronosticator);
  }

  /** Clear in-memory assignments (useful for tests). */
  public void clearAssignments() {
    playersByPronosticator.clear();
  }

  private CsvParseResult parseCsvRows() {
    ClassPathResource resource = new ClassPathResource(CSV_RESOURCE_PATH);
    List<CsvRow> rows = List.of();
    int errorCount = 0;
    String failureReason = null;

    if (!resource.exists()) {
      failureReason = "file_not_found";
      log.warn("CSV parse failed: reason=file_not_found, path={}", CSV_RESOURCE_PATH);
    } else {
      CsvReadResult readResult = readRowsFromResource(resource);
      rows = readResult.rows();
      errorCount = readResult.errorCount();
      failureReason = readResult.failureReason();

      if (failureReason == null && rows.isEmpty()) {
        failureReason = "no_rows";
        log.warn("CSV parse failed: reason=no_rows");
      }
    }

    if (failureReason != null) {
      return CsvParseResult.failure(failureReason, errorCount);
    }

    log.info("CSV parse ok: rows={}, errors={}", rows.size(), errorCount);
    return CsvParseResult.success(rows, errorCount);
  }

  private CsvReadResult readRowsFromResource(ClassPathResource resource) {
    List<CsvRow> rows = new ArrayList<>();
    int errorCount = 0;

    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

      String header = reader.readLine();
      if (!isHeaderValid(header)) {
        log.warn("CSV parse failed: reason=invalid_header, header={}", header);
        return CsvReadResult.failure("invalid_header", errorCount);
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
        } else {
          rows.add(row);
        }
      }
    } catch (IOException e) {
      log.error("CSV parse failed: reason=io_error", e);
      return CsvReadResult.failure("io_error", errorCount);
    }

    return CsvReadResult.success(rows, errorCount);
  }

  private boolean isHeaderValid(String header) {
    return header != null && header.contains(REQUIRED_HEADER_TOKEN);
  }

  private CsvRow parseCsvLine(String line, int lineNumber) {
    String[] parts = line.split(",");
    if (parts.length < EXPECTED_COLUMNS) {
      log.warn("CSV parse failed: line={}, reason=invalid_columns, value={}", lineNumber, line);
      return null;
    }

    String pronostiqueur = cleanField(parts[COLUMN_PRONOSTIQUEUR_INDEX]);
    String nickname = cleanField(parts[COLUMN_NICKNAME_INDEX]);
    String region = cleanField(parts[COLUMN_REGION_INDEX]);
    if (pronostiqueur.isEmpty() || nickname.isEmpty() || region.isEmpty()) {
      log.warn("CSV parse failed: line={}, reason=missing_required, value={}", lineNumber, line);
      return null;
    }

    int points = parseIntSafely(cleanField(parts[COLUMN_POINTS_INDEX]), DEFAULT_NUMERIC_VALUE);
    int classement = parseIntSafely(cleanField(parts[COLUMN_RANKING_INDEX]), DEFAULT_NUMERIC_VALUE);
    String resolvedTranche = resolveTranche(cleanField(parts[COLUMN_TRANCHE_INDEX]));
    com.fortnite.pronos.model.Player.Region playerRegion = parseRegion(region);

    return new CsvRow(pronostiqueur, nickname, playerRegion, points, classement, resolvedTranche);
  }

  private String resolveTranche(String tranche) {
    return tranche.isEmpty() ? DEFAULT_TRANCHE : tranche;
  }

  private CsvSaveResult saveCsvRows(List<CsvRow> rows) {
    Map<String, com.fortnite.pronos.model.Player> playersByNickname = loadPlayersByNickname();
    List<com.fortnite.pronos.model.Player> newPlayers = new ArrayList<>();
    int updatedPlayers = upsertPlayers(rows, playersByNickname, newPlayers);

    persistNewPlayers(newPlayers, playersByNickname);
    buildPronosticatorAssignments(rows, playersByNickname);

    ScoreSyncResult scoreSync = upsertScores(rows, playersByNickname, loadScoresByPlayerId());

    return new CsvSaveResult(
        newPlayers.size(), updatedPlayers, scoreSync.newScores(), scoreSync.updatedScores());
  }

  private int upsertPlayers(
      List<CsvRow> rows,
      Map<String, com.fortnite.pronos.model.Player> playersByNickname,
      List<com.fortnite.pronos.model.Player> newPlayers) {
    int updatedPlayers = 0;
    for (CsvRow row : rows) {
      String key = normalizeNickname(row.nickname());
      com.fortnite.pronos.model.Player player = playersByNickname.get(key);
      if (player == null) {
        player = buildPlayer(row);
        newPlayers.add(player);
        playersByNickname.put(key, player);
        continue;
      }
      if (updatePlayerFromRow(player, row)) {
        updatedPlayers++;
      }
    }
    return updatedPlayers;
  }

  private void persistNewPlayers(
      List<com.fortnite.pronos.model.Player> newPlayers,
      Map<String, com.fortnite.pronos.model.Player> playersByNickname) {
    if (newPlayers.isEmpty()) {
      return;
    }
    List<com.fortnite.pronos.model.Player> savedPlayers = playerRepository.saveAll(newPlayers);
    for (com.fortnite.pronos.model.Player saved : savedPlayers) {
      playersByNickname.put(normalizeNickname(saved.getNickname()), saved);
    }
  }

  private void buildPronosticatorAssignments(
      List<CsvRow> rows, Map<String, com.fortnite.pronos.model.Player> playersByNickname) {
    for (CsvRow row : rows) {
      com.fortnite.pronos.model.Player player =
          playersByNickname.get(normalizeNickname(row.nickname()));
      if (player == null) {
        continue;
      }
      playersByPronosticator
          .computeIfAbsent(row.pronostiqueur(), keyValue -> new ArrayList<>())
          .add(player);
    }
  }

  private ScoreSyncResult upsertScores(
      List<CsvRow> rows,
      Map<String, com.fortnite.pronos.model.Player> playersByNickname,
      Map<UUID, com.fortnite.pronos.model.Score> scoresByPlayerId) {
    List<com.fortnite.pronos.model.Score> newScores = new ArrayList<>();
    int updatedScores = 0;
    for (CsvRow row : rows) {
      com.fortnite.pronos.model.Player player =
          playersByNickname.get(normalizeNickname(row.nickname()));
      if (player == null || player.getId() == null) {
        continue;
      }
      com.fortnite.pronos.model.Score score = scoresByPlayerId.get(player.getId());
      if (score == null) {
        newScores.add(buildScore(player, row));
        continue;
      }
      if (score.getPoints() == null || score.getPoints() != row.points()) {
        score.setPoints(row.points());
        updatedScores++;
      }
    }
    if (!newScores.isEmpty()) {
      scoreRepository.saveAll(newScores);
    }
    return new ScoreSyncResult(newScores.size(), updatedScores);
  }

  private Map<String, com.fortnite.pronos.model.Player> loadPlayersByNickname() {
    Map<String, com.fortnite.pronos.model.Player> result = new HashMap<>();
    for (com.fortnite.pronos.model.Player player : playerRepository.findAll()) {
      result.putIfAbsent(normalizeNickname(player.getNickname()), player);
    }
    return result;
  }

  private Map<UUID, com.fortnite.pronos.model.Score> loadScoresByPlayerId() {
    Map<UUID, com.fortnite.pronos.model.Score> result = new HashMap<>();
    for (com.fortnite.pronos.model.Score score : scoreRepository.findBySeason(CURRENT_SEASON)) {
      if (score.getPlayer() != null && score.getPlayer().getId() != null) {
        result.putIfAbsent(score.getPlayer().getId(), score);
      }
    }
    return result;
  }

  private com.fortnite.pronos.model.Player buildPlayer(CsvRow row) {
    String cleanUsername = buildUsername(row.nickname());
    return com.fortnite.pronos.model.Player.builder()
        .username(cleanUsername)
        .nickname(row.nickname())
        .region(row.region())
        .tranche(row.tranche())
        .currentSeason(CURRENT_SEASON)
        .build();
  }

  private boolean updatePlayerFromRow(com.fortnite.pronos.model.Player player, CsvRow row) {
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

  private com.fortnite.pronos.model.Score buildScore(
      com.fortnite.pronos.model.Player player, CsvRow row) {
    com.fortnite.pronos.model.Score score = new com.fortnite.pronos.model.Score();
    score.setPlayer(player);
    score.setSeason(CURRENT_SEASON);
    score.setPoints(row.points());
    score.setDate(LocalDate.now());
    score.setTimestamp(OffsetDateTime.now());
    return score;
  }

  private String normalizeNickname(String nickname) {
    return nickname == null ? EMPTY_STRING : nickname.trim().toLowerCase(Locale.ROOT);
  }

  private String cleanField(String field) {
    return field == null ? EMPTY_STRING : field.trim().replace("\"", EMPTY_STRING);
  }

  private int parseIntSafely(String value, int defaultValue) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private com.fortnite.pronos.model.Player.Region parseRegion(String region) {
    try {
      return com.fortnite.pronos.model.Player.Region.valueOf(region.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      log.warn("CSV parse: invalid region '{}', defaulting to EU", region);
      return com.fortnite.pronos.model.Player.Region.EU;
    }
  }

  private String buildUsername(String nickname) {
    String sourceNickname = nickname == null ? EMPTY_STRING : nickname;
    String cleanUsername =
        NON_ALPHANUMERIC_PATTERN
            .matcher(sourceNickname.toLowerCase(Locale.ROOT))
            .replaceAll(EMPTY_STRING);
    if (cleanUsername.isEmpty()) {
      cleanUsername = USERNAME_FALLBACK_PREFIX + Math.abs(sourceNickname.hashCode());
    }
    return cleanUsername;
  }

  private record CsvRow(
      String pronostiqueur,
      String nickname,
      com.fortnite.pronos.model.Player.Region region,
      int points,
      int classement,
      String tranche) {}

  private record CsvParseResult(List<CsvRow> rows, int errorCount, String failureReason) {
    static CsvParseResult success(List<CsvRow> rows, int errorCount) {
      return new CsvParseResult(rows, errorCount, null);
    }

    static CsvParseResult failure(String reason, int errorCount) {
      return new CsvParseResult(List.of(), errorCount, reason);
    }

    boolean hasRows() {
      return rows != null && !rows.isEmpty();
    }
  }

  private record CsvReadResult(List<CsvRow> rows, int errorCount, String failureReason) {
    static CsvReadResult success(List<CsvRow> rows, int errorCount) {
      return new CsvReadResult(rows, errorCount, null);
    }

    static CsvReadResult failure(String reason, int errorCount) {
      return new CsvReadResult(List.of(), errorCount, reason);
    }
  }

  private record CsvSaveResult(
      int newPlayers, int updatedPlayers, int newScores, int updatedScores) {}

  private record ScoreSyncResult(int newScores, int updatedScores) {}
}
