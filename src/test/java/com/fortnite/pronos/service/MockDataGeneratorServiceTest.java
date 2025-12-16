package com.fortnite.pronos.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fortnite.pronos.model.Player;

/**
 * Tests unitaires TDD pour MockDataGeneratorService Clean Code : Tests focalisés sur la génération
 * de données mock
 */
@DisplayName("MockDataGeneratorService - Tests unitaires")
class MockDataGeneratorServiceTest {

  private MockDataGeneratorService service;

  @BeforeEach
  void setUp() {
    service = new MockDataGeneratorService();
  }

  @Test
  @DisplayName("GIVEN CSV file exists WHEN loadMockDataFromCsv THEN should load all players")
  void testLoadMockDataFromCsv_WithValidCsv_ShouldLoadPlayers() {
    // WHEN
    MockDataGeneratorService.MockDataSet result = service.loadMockDataFromCsv();

    // THEN
    assertThat(result).isNotNull();
    assertThat(result.total()).isGreaterThan(0);
    assertThat(result.getAllPlayers()).isNotEmpty();
    assertThat(result.getAllScores()).isNotEmpty();
    assertThat(result.getPronosticators()).isNotEmpty();
  }

  @Test
  @DisplayName(
      "GIVEN valid CSV WHEN loadMockDataFromCsv THEN should have Marcel, Thibaut, Teddy as pronosticators")
  void testLoadMockDataFromCsv_ShouldHaveExpectedPronosticators() {
    // WHEN
    MockDataGeneratorService.MockDataSet result = service.loadMockDataFromCsv();

    // THEN
    assertThat(result.getPronosticators())
        .containsAnyOf("Marcel", "Thibaut", "Teddy")
        .as("Should contain at least one of the expected pronosticators");
  }

  @Test
  @DisplayName("GIVEN loaded data WHEN getPlayersFor THEN should return players for pronostiqueur")
  void testGetPlayersFor_WithValidPronostiqueur_ShouldReturnPlayers() {
    // GIVEN
    MockDataGeneratorService.MockDataSet mockData = service.loadMockDataFromCsv();
    String pronostiqueur = mockData.getPronosticators().get(0);

    // WHEN
    var players = mockData.getPlayersFor(pronostiqueur);

    // THEN
    assertThat(players).isNotEmpty();
    assertThat(players.get(0).pronostiqueur()).isEqualTo(pronostiqueur);
  }

  @Test
  @DisplayName("GIVEN loaded data WHEN getAllPlayers THEN should return all unique players")
  void testGetAllPlayers_ShouldReturnAllPlayers() {
    // GIVEN
    MockDataGeneratorService.MockDataSet mockData = service.loadMockDataFromCsv();

    // WHEN
    var allPlayers = mockData.getAllPlayers();

    // THEN
    assertThat(allPlayers).isNotEmpty();
    assertThat(allPlayers).allMatch(player -> player.getUsername() != null);
    assertThat(allPlayers).allMatch(player -> player.getNickname() != null);
    assertThat(allPlayers).allMatch(player -> player.getRegion() != null);
  }

  @Test
  @DisplayName("GIVEN loaded data WHEN getAllScores THEN should have scores for all players")
  void testGetAllScores_ShouldHaveScoresForAllPlayers() {
    // GIVEN
    MockDataGeneratorService.MockDataSet mockData = service.loadMockDataFromCsv();

    // WHEN
    var allScores = mockData.getAllScores();

    // THEN
    assertThat(allScores).isNotEmpty();
    assertThat(allScores).hasSameSizeAs(mockData.getAllPlayers());
    assertThat(allScores).allMatch(score -> score.getPoints() >= 0);
    assertThat(allScores).allMatch(score -> score.getSeason() == 2025);
  }

  @Test
  @DisplayName("GIVEN invalid pronostiqueur WHEN getPlayersFor THEN should return empty list")
  void testGetPlayersFor_WithInvalidPronostiqueur_ShouldReturnEmpty() {
    // GIVEN
    MockDataGeneratorService.MockDataSet mockData = service.loadMockDataFromCsv();

    // WHEN
    var players = mockData.getPlayersFor("NonExistentPronostiqueur");

    // THEN
    assertThat(players).isEmpty();
  }

  @Test
  @DisplayName("GIVEN empty MockDataSet WHEN calling methods THEN should return empty results")
  void testEmptyMockDataSet_ShouldReturnEmptyResults() {
    // GIVEN
    MockDataGeneratorService.MockDataSet emptyData = MockDataGeneratorService.MockDataSet.empty();

    // THEN
    assertThat(emptyData.total()).isZero();
    assertThat(emptyData.getAllPlayers()).isEmpty();
    assertThat(emptyData.getAllScores()).isEmpty();
    assertThat(emptyData.getPronosticators()).isEmpty();
  }

  @Test
  @DisplayName(
      "GIVEN loaded data WHEN checking player regions THEN should have valid region values")
  void testLoadedPlayers_ShouldHaveValidRegions() {
    // GIVEN
    MockDataGeneratorService.MockDataSet mockData = service.loadMockDataFromCsv();

    // WHEN
    var allPlayers = mockData.getAllPlayers();

    // THEN
    assertThat(allPlayers)
        .allMatch(
            player ->
                player.getRegion() == Player.Region.EU
                    || player.getRegion() == Player.Region.NAC
                    || player.getRegion() == Player.Region.NAW
                    || player.getRegion() == Player.Region.ASIA
                    || player.getRegion() == Player.Region.OCE
                    || player.getRegion() == Player.Region.BR
                    || player.getRegion() == Player.Region.ME);
  }

  @Test
  @DisplayName(
      "GIVEN CSV with special characters WHEN parsing THEN should generate valid usernames")
  void testParsing_WithSpecialCharacters_ShouldGenerateValidUsernames() {
    // GIVEN
    MockDataGeneratorService.MockDataSet mockData = service.loadMockDataFromCsv();

    // WHEN
    var allPlayers = mockData.getAllPlayers();

    // THEN
    assertThat(allPlayers)
        .allMatch(player -> player.getUsername().matches("[a-z0-9]+"))
        .as("All usernames should be alphanumeric lowercase");
  }
}
