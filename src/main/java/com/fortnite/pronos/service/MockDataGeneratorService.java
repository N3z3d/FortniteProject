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

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service de génération de données mock depuis le fichier CSV Clean Code : Service focalisé sur la
 * génération de données de test (SRP)
 */
@Service
@Slf4j
@SuppressWarnings({"java:S1141", "java:S135", "java:S2676"})
public class MockDataGeneratorService {

  /**
   * Charge et retourne les données mock depuis le CSV
   *
   * @return Map contenant les joueurs par pronostiqueur
   */
  public MockDataSet loadMockDataFromCsv() {
    try {
      log.info("[MOCK] Chargement des donnees mock depuis le CSV...");

      ClassPathResource resource = new ClassPathResource("data/fortnite_data.csv");

      if (!resource.exists()) {
        log.warn("[WARN] Fichier CSV non trouvé: data/fortnite_data.csv");
        return MockDataSet.empty();
      }

      Map<String, List<PlayerWithScore>> playersByPronosticator = new HashMap<>();
      int totalPlayers = 0;

      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

        String headerLine = reader.readLine();
        if (headerLine == null || !headerLine.contains("Pronostiqueur")) {
          log.warn("[WARN] Format CSV invalide - header manquant ou incorrect");
          return MockDataSet.empty();
        }

        int lineNumber = 1;
        String line;
        while ((line = reader.readLine()) != null) {
          lineNumber++;
          try {
            if (line.trim().isEmpty()) {
              continue;
            }

            PlayerWithScore playerData = parseCsvLine(line, lineNumber);
            if (playerData == null) {
              continue;
            }

            playersByPronosticator
                .computeIfAbsent(playerData.pronostiqueur(), k -> new ArrayList<>())
                .add(playerData);
            totalPlayers++;

          } catch (Exception e) {
            log.warn("Erreur ligne {} : {}", lineNumber, e.getMessage());
          }
        }
      }

      MockDataSet result = new MockDataSet(playersByPronosticator, totalPlayers);

      log.info("[OK] Données mock chargées avec succès:");
      log.info("   - {} joueurs au total", totalPlayers);
      log.info("   - Répartition:");
      playersByPronosticator.forEach(
          (prono, players) -> log.info("     • {}: {} joueurs", prono, players.size()));

      return result;

    } catch (Exception e) {
      log.error("[ERROR] Erreur lors du chargement des données mock CSV", e);
      return MockDataSet.empty();
    }
  }

  /**
   * Parse une ligne CSV et retourne les données du joueur Note: Ignore la colonne "Basé 2024" comme
   * demandé
   */
  private PlayerWithScore parseCsvLine(String line, int lineNumber) {
    String[] parts = line.split(",");

    // Format attendu: Pronostiqueur,Joueur,Région,com.fortnite.pronos.model.Score
    // PR,Classement,Basé 2024
    if (parts.length < 5) {
      log.warn(
          "Ligne {} ignorée - format invalide (attendu au moins 5 colonnes): {}", lineNumber, line);
      return null;
    }

    try {
      String pronostiqueur = cleanCsvField(parts[0]);
      String nickname = cleanCsvField(parts[1]);
      String region = cleanCsvField(parts[2]);
      String pointsStr = cleanCsvField(parts[3]);
      String classementStr = cleanCsvField(parts[4]);
      // parts[5] = "Basé 2024" - IGNORÉ comme demandé

      // Validation des champs essentiels
      if (pronostiqueur.isEmpty() || nickname.isEmpty() || region.isEmpty()) {
        log.warn("Ligne {} ignorée - données essentielles manquantes", lineNumber);
        return null;
      }

      // Parse des valeurs numériques
      int points = parseIntSafely(pointsStr, 0);
      int classement = parseIntSafely(classementStr, 0);

      // Validation de la région
      com.fortnite.pronos.model.Player.Region playerRegion = parseRegion(region);

      // Génération username valide
      String username = generateValidUsername(nickname);

      // Création du player (tranche par défaut "1-5" pour respecter validation @PrePersist)
      com.fortnite.pronos.model.Player player =
          com.fortnite.pronos.model.Player.builder()
              .username(username)
              .nickname(nickname)
              .region(playerRegion)
              .tranche("1-5") // Tranche par défaut (requis par validation @NotBlank)
              .currentSeason(2025)
              .build();

      // Création du score
      com.fortnite.pronos.model.Score score = new com.fortnite.pronos.model.Score();
      score.setPlayer(player);
      score.setSeason(2025);
      score.setPoints(points);
      score.setDate(LocalDate.now());
      score.setTimestamp(OffsetDateTime.now());

      return new PlayerWithScore(pronostiqueur, player, score, classement);

    } catch (Exception e) {
      log.warn("Erreur parsing ligne {}: {}", lineNumber, e.getMessage());
      return null;
    }
  }

  /** Nettoie un champ CSV (supprime guillemets et espaces) */
  private String cleanCsvField(String field) {
    return field.trim().replace("\"", "");
  }

  /** Parse un entier de manière sécurisée avec valeur par défaut */
  private int parseIntSafely(String value, int defaultValue) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /** Parse une région avec fallback vers EU */
  private com.fortnite.pronos.model.Player.Region parseRegion(String region) {
    try {
      return com.fortnite.pronos.model.Player.Region.valueOf(region.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("Région invalide '{}', utilisation de EU par défaut", region);
      return com.fortnite.pronos.model.Player.Region.EU;
    }
  }

  /** Génère un username valide à partir du nickname */
  private String generateValidUsername(String nickname) {
    if (nickname == null || nickname.trim().isEmpty()) {
      return "player" + System.currentTimeMillis();
    }

    // Nettoyer le nickname pour créer un username valide (alphanumeric only)
    String cleaned = nickname.toLowerCase().replaceAll("[^a-z0-9]", "");

    // Si vide après nettoyage, générer un username basé sur le hash
    if (cleaned.isEmpty()) {
      return "player" + Math.abs(nickname.hashCode());
    }

    // S'assurer d'une longueur minimale
    if (cleaned.length() < 3) {
      cleaned = cleaned + "usr" + (System.currentTimeMillis() % 1000);
    }

    return cleaned;
  }

  /** Record représentant un joueur avec son score */
  public record PlayerWithScore(
      String pronostiqueur,
      com.fortnite.pronos.model.Player player,
      com.fortnite.pronos.model.Score score,
      int classement) {}

  /** Record représentant l'ensemble des données mock */
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
