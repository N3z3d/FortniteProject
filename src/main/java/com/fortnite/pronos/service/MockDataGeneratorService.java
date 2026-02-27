package com.fortnite.pronos.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/** Service de generation de donnees mock depuis le fichier CSV. */
@Service
@Slf4j
@SuppressWarnings({"java:S1141", "java:S135", "java:S2676"})
public class MockDataGeneratorService {

  private static final String CSV_RESOURCE_PATH = "data/fortnite_data.csv";
  private static final String PRONOSTIQUEUR_HEADER = "Pronostiqueur";
  private static final int MINIMUM_EXPECTED_COLUMNS = 5;
  private static final int PRONOSTIQUEUR_INDEX = 0;
  private static final int NICKNAME_INDEX = 1;
  private static final int REGION_INDEX = 2;
  private static final int POINTS_INDEX = 3;
  private static final int RANKING_INDEX = 4;
  private static final int DEFAULT_INT_VALUE = 0;
  private static final int CURRENT_SEASON = 2025;
  private static final String DEFAULT_TRANCHE = "1-5";
  private static final String PLAYER_USERNAME_PREFIX = "player";
  private static final int MIN_USERNAME_LENGTH = 3;
  private static final String SHORT_USERNAME_SUFFIX = "usr";
  private static final int USERNAME_SUFFIX_MODULO = 1000;
  private static final Pattern NON_ALPHANUMERIC_PATTERN = Pattern.compile("[^a-z0-9]");

  /**
   * Charge et retourne les donnees mock depuis le CSV.
   *
   * @return Map contenant les joueurs par pronostiqueur
   */
  public MockDataSet loadMockDataFromCsv() {
    MockDataSet result = MockDataSet.empty();
    log.info("[MOCK] Chargement des donnees mock depuis le CSV...");

    ClassPathResource resource = new ClassPathResource(CSV_RESOURCE_PATH);
    if (!resource.exists()) {
      log.warn("[WARN] Fichier CSV non trouve: {}", CSV_RESOURCE_PATH);
      return result;
    }

    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

      String headerLine = reader.readLine();
      if (!isValidHeader(headerLine)) {
        log.warn("[WARN] Format CSV invalide - header manquant ou incorrect");
      } else {
        result = readPlayers(reader);
        logLoadedData(result);
      }

    } catch (Exception e) {
      log.error("[ERROR] Erreur lors du chargement des donnees mock CSV", e);
    }

    return result;
  }

  /** Parse une ligne CSV et retourne les donnees du joueur. */
  private PlayerWithScore parseCsvLine(String line, int lineNumber) {
    String[] parts = line.split(",");
    if (parts.length < MINIMUM_EXPECTED_COLUMNS) {
      log.warn(
          "Ligne {} ignoree - format invalide (attendu au moins 5 colonnes): {}", lineNumber, line);
      return null;
    }

    try {
      CsvRow csvRow = toCsvRow(parts);
      if (!hasRequiredFields(csvRow)) {
        log.warn("Ligne {} ignoree - donnees essentielles manquantes", lineNumber);
      } else {
        return toPlayerWithScore(csvRow);
      }
    } catch (Exception e) {
      log.warn("Erreur parsing ligne {}: {}", lineNumber, e.getMessage());
    }

    return null;
  }

  /** Nettoie un champ CSV (supprime guillemets et espaces). */
  private String cleanCsvField(String field) {
    return field.trim().replace("\"", "");
  }

  /** Parse un entier de maniere securisee avec valeur par defaut. */
  private int parseIntSafely(String value, int defaultValue) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /** Parse une region avec fallback vers EU. */
  private com.fortnite.pronos.model.Player.Region parseRegion(String region) {
    try {
      return com.fortnite.pronos.model.Player.Region.valueOf(region.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      log.warn("Region invalide '{}', utilisation de EU par defaut", region);
      return com.fortnite.pronos.model.Player.Region.EU;
    }
  }

  /** Genere un username valide a partir du nickname. */
  private String generateValidUsername(String nickname) {
    if (nickname == null || nickname.trim().isEmpty()) {
      return PLAYER_USERNAME_PREFIX + System.currentTimeMillis();
    }

    String cleaned =
        NON_ALPHANUMERIC_PATTERN.matcher(nickname.toLowerCase(Locale.ROOT)).replaceAll("");

    if (cleaned.isEmpty()) {
      return PLAYER_USERNAME_PREFIX + Math.abs(nickname.hashCode());
    }

    if (cleaned.length() < MIN_USERNAME_LENGTH) {
      cleaned =
          cleaned + SHORT_USERNAME_SUFFIX + (System.currentTimeMillis() % USERNAME_SUFFIX_MODULO);
    }

    return cleaned;
  }

  private boolean isValidHeader(String headerLine) {
    return headerLine != null && headerLine.contains(PRONOSTIQUEUR_HEADER);
  }

  private MockDataSet readPlayers(BufferedReader reader) throws java.io.IOException {
    Map<String, List<PlayerWithScore>> playersByPronosticator = new HashMap<>();
    int totalPlayers = 0;
    int lineNumber = 1;

    String line;
    while ((line = reader.readLine()) != null) {
      lineNumber++;
      if (line.trim().isEmpty()) {
        continue;
      }

      PlayerWithScore playerData = parseCsvLine(line, lineNumber);
      if (playerData == null) {
        continue;
      }

      playersByPronosticator
          .computeIfAbsent(playerData.pronostiqueur(), unusedKey -> new ArrayList<>())
          .add(playerData);
      totalPlayers++;
    }

    return new MockDataSet(playersByPronosticator, totalPlayers);
  }

  private void logLoadedData(MockDataSet result) {
    log.info("[OK] Donnees mock chargees avec succes:");
    log.info("   - {} joueurs au total", result.total());
    log.info("   - Repartition:");
    result
        .playersByPronosticator()
        .forEach((prono, players) -> log.info("     - {}: {} joueurs", prono, players.size()));
  }

  private CsvRow toCsvRow(String[] parts) {
    return new CsvRow(
        cleanCsvField(parts[PRONOSTIQUEUR_INDEX]),
        cleanCsvField(parts[NICKNAME_INDEX]),
        cleanCsvField(parts[REGION_INDEX]),
        cleanCsvField(parts[POINTS_INDEX]),
        cleanCsvField(parts[RANKING_INDEX]));
  }

  private boolean hasRequiredFields(CsvRow row) {
    return !row.pronostiqueur().isEmpty() && !row.nickname().isEmpty() && !row.region().isEmpty();
  }

  private PlayerWithScore toPlayerWithScore(CsvRow row) {
    int points = parseIntSafely(row.points(), DEFAULT_INT_VALUE);
    int classement = parseIntSafely(row.ranking(), DEFAULT_INT_VALUE);
    com.fortnite.pronos.model.Player.Region playerRegion = parseRegion(row.region());
    String username = generateValidUsername(row.nickname());

    com.fortnite.pronos.model.Player player =
        com.fortnite.pronos.model.Player.builder()
            .username(username)
            .nickname(row.nickname())
            .region(playerRegion)
            .tranche(DEFAULT_TRANCHE)
            .currentSeason(CURRENT_SEASON)
            .build();

    com.fortnite.pronos.model.Score score = new com.fortnite.pronos.model.Score();
    score.setPlayer(player);
    score.setSeason(CURRENT_SEASON);
    score.setPoints(points);
    score.setDate(LocalDate.now());
    score.setTimestamp(OffsetDateTime.now());

    return new PlayerWithScore(row.pronostiqueur(), player, score, classement);
  }

  private record CsvRow(
      String pronostiqueur, String nickname, String region, String points, String ranking) {}

  /** Record representant un joueur avec son score. */
  public record PlayerWithScore(
      String pronostiqueur,
      com.fortnite.pronos.model.Player player,
      com.fortnite.pronos.model.Score score,
      int classement) {}

  /** Record representant l'ensemble des donnees mock. */
  public record MockDataSet(Map<String, List<PlayerWithScore>> playersByPronosticator, int total) {
    public static MockDataSet empty() {
      return new MockDataSet(new HashMap<>(), 0);
    }

    public List<String> getPronosticators() {
      return new ArrayList<>(playersByPronosticator.keySet());
    }

    public List<PlayerWithScore> getPlayersFor(String pronostiqueur) {
      return playersByPronosticator.getOrDefault(pronostiqueur, new ArrayList<>());
    }

    public List<com.fortnite.pronos.model.Player> getAllPlayers() {
      return playersByPronosticator.values().stream()
          .flatMap(List::stream)
          .map(PlayerWithScore::player)
          .toList();
    }

    public List<com.fortnite.pronos.model.Score> getAllScores() {
      return playersByPronosticator.values().stream()
          .flatMap(List::stream)
          .map(PlayerWithScore::score)
          .toList();
    }
  }
}
