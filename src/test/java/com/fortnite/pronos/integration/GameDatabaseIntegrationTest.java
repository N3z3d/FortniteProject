package com.fortnite.pronos.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Tests d'intégration pour vérifier les tables de games en base de données */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Tests d'intégration - Tables Games")
class GameDatabaseIntegrationTest {

  @Autowired private DataSource dataSource;

  @Test
  @DisplayName("Devrait avoir les tables de games créées")
  @Transactional
  void shouldHaveGamesTablesCreated() throws Exception {
    // Given
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {

      // When - Vérifier que la table games existe
      ResultSet resultSet =
          statement.executeQuery(
              "SELECT COUNT(*) FROM information_schema.tables "
                  + "WHERE table_name = 'games' AND table_schema = 'public'");

      // Then
      assertTrue(resultSet.next());
      assertEquals(1, resultSet.getInt(1), "La table 'games' devrait exister");
    }
  }

  @Test
  @DisplayName("Devrait avoir la table game_region_rules créée")
  @Transactional
  void shouldHaveGameRegionRulesTableCreated() throws Exception {
    // Given
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {

      // When - Vérifier que la table game_region_rules existe
      ResultSet resultSet =
          statement.executeQuery(
              "SELECT COUNT(*) FROM information_schema.tables "
                  + "WHERE table_name = 'game_region_rules' AND table_schema = 'public'");

      // Then
      assertTrue(resultSet.next());
      assertEquals(1, resultSet.getInt(1), "La table 'game_region_rules' devrait exister");
    }
  }

  @Test
  @DisplayName("Devrait avoir la table game_participants créée")
  @Transactional
  void shouldHaveGameParticipantsTableCreated() throws Exception {
    // Given
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {

      // When - Vérifier que la table game_participants existe
      ResultSet resultSet =
          statement.executeQuery(
              "SELECT COUNT(*) FROM information_schema.tables "
                  + "WHERE table_name = 'game_participants' AND table_schema = 'public'");

      // Then
      assertTrue(resultSet.next());
      assertEquals(1, resultSet.getInt(1), "La table 'game_participants' devrait exister");
    }
  }

  @Test
  @DisplayName("Devrait avoir la table game_participant_players créée")
  @Transactional
  void shouldHaveGameParticipantPlayersTableCreated() throws Exception {
    // Given
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {

      // When - Vérifier que la table game_participant_players existe
      ResultSet resultSet =
          statement.executeQuery(
              "SELECT COUNT(*) FROM information_schema.tables "
                  + "WHERE table_name = 'game_participant_players' AND table_schema = 'public'");

      // Then
      assertTrue(resultSet.next());
      assertEquals(1, resultSet.getInt(1), "La table 'game_participant_players' devrait exister");
    }
  }

  @Test
  @DisplayName("Devrait avoir les contraintes de clés étrangères")
  @Transactional
  void shouldHaveForeignKeyConstraints() throws Exception {
    // Given
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {

      // When - Vérifier les contraintes de clés étrangères
      ResultSet resultSet =
          statement.executeQuery(
              "SELECT COUNT(*) FROM information_schema.table_constraints "
                  + "WHERE constraint_type = 'FOREIGN KEY' "
                  + "AND table_name IN ('games', 'game_region_rules', 'game_participants', 'game_participant_players')");

      // Then
      assertTrue(resultSet.next());
      int foreignKeyCount = resultSet.getInt(1);
      assertTrue(foreignKeyCount >= 4, "Devrait avoir au moins 4 clés étrangères");
    }
  }

  /*
   * TODO: Test skipped for H2 - Run against PostgreSQL in production
   *
   * CONTEXT:
   * - H2 (test database) and PostgreSQL (production) handle indexes differently
   * - H2 creates automatic indexes on foreign keys, but they may have different naming conventions
   * - Index names in H2 (like PRIMARY_KEY_*) differ from PostgreSQL custom index names (IDX_GAMES_*)
   * - This causes false positives/negatives when counting indexes in H2
   *
   * ACTION REQUIRED:
   * 1. Add this test to a separate PostgreSQL-only test suite (e.g., @Tag("postgresql"))
   * 2. Configure CI/CD to run PostgreSQL integration tests with Testcontainers:
   *    - Use @Testcontainers with PostgreSQLContainer
   *    - Or run against real PostgreSQL instance in CI
   * 3. Verify the following indexes exist in PostgreSQL:
   *    - IDX_GAMES_STATUS (on games.status)
   *    - IDX_GAMES_START_DATE (on games.start_date)
   *    - IDX_GAME_REGION_RULES_GAME_ID (on game_region_rules.game_id)
   *    - IDX_GAME_PARTICIPANTS_GAME_ID (on game_participants.game_id)
   *    - IDX_GAME_PARTICIPANTS_USER_ID (on game_participants.user_id)
   *    - IDX_GAME_PARTICIPANT_PLAYERS_PARTICIPANT_ID (on game_participant_players.game_participant_id)
   *
   * REFERENCE:
   * - Migration file: src/main/resources/db/migration/V1__clean_schema.sql
   * - Search for "CREATE INDEX" to see all expected indexes
   *
   * ESTIMATED EFFORT: 2-3 hours
   * - Setup Testcontainers: 1h
   * - Write PostgreSQL-specific test: 30min
   * - Configure CI pipeline: 1-2h
   */
  @Test
  @Disabled(
      "H2 and PostgreSQL handle indexes differently - must run against PostgreSQL. See TODO above for details.")
  @DisplayName("Devrait avoir les index de performance")
  @Transactional
  void shouldHavePerformanceIndexes() throws Exception {
    // Given
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {

      // When - Vérifier les index (H2 compatible)
      ResultSet resultSet =
          statement.executeQuery(
              "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES "
                  + "WHERE INDEX_NAME LIKE 'IDX_GAMES%' "
                  + "OR INDEX_NAME LIKE 'IDX_GAME_%'");

      // Then
      assertTrue(resultSet.next());
      int indexCount = resultSet.getInt(1);
      assertTrue(indexCount >= 6, "Devrait avoir au moins 6 index de performance");
    }
  }
}
