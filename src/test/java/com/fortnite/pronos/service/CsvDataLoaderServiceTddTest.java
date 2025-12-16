package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.Score;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.ScoreRepository;

/**
 * Tests TDD pour CsvDataLoaderService Valide que tous les joueurs du CSV sont bien chargés en base
 */
@SpringBootTest(
    classes = {
      com.fortnite.pronos.PronosApplication.class,
      com.fortnite.pronos.config.TestSecurityConfig.class
    })
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Sql(
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
    statements = {
      "SET REFERENTIAL_INTEGRITY FALSE",
      "DELETE FROM scores",
      "DELETE FROM team_players",
      "DELETE FROM teams",
      "DELETE FROM game_participants",
      "DELETE FROM games",
      "DELETE FROM players",
      "DELETE FROM users",
      "SET REFERENTIAL_INTEGRITY TRUE"
    })
@DisplayName("CsvDataLoaderService - TDD Tests")
class CsvDataLoaderServiceTddTest {

  @Autowired private CsvDataLoaderService csvDataLoaderService;

  @Autowired private PlayerRepository playerRepository;

  @Autowired private ScoreRepository scoreRepository;

  @BeforeEach
  void setUp() {
    // Clean database before each test
    scoreRepository.deleteAll();
    playerRepository.deleteAll();
  }

  @Test
  @Transactional
  @DisplayName("Devrait charger tous les 147 joueurs du fichier CSV")
  void shouldLoadAll147PlayersFromCsv() {
    // ARRANGE
    long initialPlayerCount = playerRepository.count();
    long initialScoreCount = scoreRepository.count();

    assertThat(initialPlayerCount).isEqualTo(0);
    assertThat(initialScoreCount).isEqualTo(0);

    // ACT
    csvDataLoaderService.loadAllCsvData();

    // ASSERT
    List<Player> allPlayers = playerRepository.findAll();
    List<Score> allScores = scoreRepository.findAll();

    // Le CSV contient 147 lignes de données (hors header)
    assertThat(allPlayers).hasSize(147).as("Le CSV devrait charger 147 joueurs, pas 12 hardcodés");

    assertThat(allScores).hasSize(147).as("Chaque joueur devrait avoir un score associé");

    // Vérifier que ce ne sont pas les joueurs hardcodés
    List<String> hardcodedNicknames =
        List.of(
            "Peterbot",
            "ふーくん",
            "Oatley",
            "FKS",
            "MariusCOW",
            "PXMP",
            "Eomzo",
            "Koyota",
            "Wreckless",
            "KING",
            "Parz",
            "Kchorro");

    List<String> loadedNicknames = allPlayers.stream().map(Player::getNickname).toList();

    // Vérifier qu'on a des joueurs du CSV, pas seulement les hardcodés
    assertThat(loadedNicknames)
        .as("Devrait contenir des joueurs du CSV comme 'pixie', 'Muz', 'White', etc.")
        .contains("pixie", "Muz", "White", "5aald", "Nuti")
        .hasSizeGreaterThan(12);

    System.out.println("✅ TEST PASSED: " + allPlayers.size() + " joueurs chargés du CSV");
    System.out.println(
        "✅ Premiers joueurs: " + loadedNicknames.subList(0, Math.min(5, loadedNicknames.size())));
  }

  @Test
  @Transactional
  @DisplayName("Devrait créer des scores avec les points réels du CSV")
  void shouldCreateScoresWithRealPointsFromCsv() {
    // ARRANGE & ACT
    csvDataLoaderService.loadAllCsvData();

    // ASSERT
    List<Score> allScores = scoreRepository.findAll();
    assertThat(allScores).isNotEmpty();

    // Vérifier quelques scores spécifiques du CSV
    Player pixiePlayer = playerRepository.findByNickname("pixie").orElse(null);
    if (pixiePlayer != null) {
      List<Score> pixieScores = scoreRepository.findByPlayer(pixiePlayer);
      assertThat(pixieScores).hasSize(1);
      assertThat(pixieScores.get(0).getPoints())
          .isEqualTo(108022)
          .as("pixie devrait avoir 108022 points selon le CSV");
    }

    Player muzPlayer = playerRepository.findByNickname("Muz").orElse(null);
    if (muzPlayer != null) {
      List<Score> muzScores = scoreRepository.findByPlayer(muzPlayer);
      assertThat(muzScores).hasSize(1);
      assertThat(muzScores.get(0).getPoints())
          .isEqualTo(125360)
          .as("Muz devrait avoir 125360 points selon le CSV");
    }

    System.out.println("✅ TEST PASSED: Scores avec points réels du CSV créés");
  }

  @Test
  @Transactional
  @DisplayName("Ne devrait PAS utiliser les 12 joueurs hardcodés")
  void shouldNotUseHardcodedPlayers() {
    // ACT
    csvDataLoaderService.loadAllCsvData();

    // ASSERT
    List<Player> allPlayers = playerRepository.findAll();

    // Si on a exactement 12 joueurs, c'est un problème !
    assertThat(allPlayers)
        .hasSizeGreaterThan(12)
        .as("ERREUR: Seuls les 12 joueurs hardcodés ont été chargés au lieu du CSV complet");

    // Vérifier la diversité des régions (le CSV a 7 régions)
    long distinctRegions = allPlayers.stream().map(Player::getRegion).distinct().count();

    assertThat(distinctRegions)
        .isGreaterThanOrEqualTo(6)
        .as("Le CSV devrait avoir des joueurs de plusieurs régions");

    System.out.println(
        "✅ TEST PASSED: Plus de " + allPlayers.size() + " joueurs chargés (pas les 12 hardcodés)");
  }

  @Test
  @DisplayName("Devrait diagnostiquer le problème si seulement 12 joueurs sont chargés")
  void shouldDiagnoseIfOnly12PlayersLoaded() {
    // ACT
    csvDataLoaderService.loadAllCsvData();

    // DIAGNOSTIC
    List<Player> allPlayers = playerRepository.findAll();
    List<Score> allScores = scoreRepository.findAll();

    System.out.println("🔍 DIAGNOSTIC:");
    System.out.println("   Players loaded: " + allPlayers.size());
    System.out.println("   Scores created: " + allScores.size());

    if (allPlayers.size() == 12) {
      System.err.println("❌ PROBLÈME DÉTECTÉ: Seulement 12 joueurs chargés");
      System.err.println("💡 Causes possibles:");
      System.err.println("   1. Transaction rollback dans DataInitializationService");
      System.err.println("   2. CsvDataLoaderService n'est pas appelé correctement");
      System.err.println("   3. Données hardcodées écrasent les données CSV");
      System.err.println("   4. Problème de lecture du fichier CSV");

      List<String> nicknames = allPlayers.stream().map(Player::getNickname).toList();
      System.err.println("   Joueurs trouvés: " + nicknames);
    } else if (allPlayers.size() >= 147) {
      System.out.println("✅ SUCCESS: " + allPlayers.size() + " joueurs chargés du CSV");
    }

    // Ce test ne fait qu'afficher le diagnostic, pas d'assertion
  }
}
