package com.fortnite.pronos.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

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

  @Test
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
