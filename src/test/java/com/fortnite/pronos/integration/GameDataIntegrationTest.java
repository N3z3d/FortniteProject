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

/** Tests d'intégration pour vérifier les données de games */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Tests d'intégration - Données Games")
class GameDataIntegrationTest {

  @Autowired private DataSource dataSource;

  @Test
  @DisplayName("Devrait avoir l'utilisateur Sarah créé")
  @Transactional
  void shouldHaveSarahUserCreated() throws Exception {
    // Given
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {

      // When - Vérifier que Sarah existe
      ResultSet resultSet =
          statement.executeQuery(
              "SELECT username, email, role FROM users WHERE username = 'Sarah'");

      // Then
      assertTrue(resultSet.next(), "Sarah devrait exister");
      assertEquals("Sarah", resultSet.getString("username"));
      assertEquals("sarah@test.com", resultSet.getString("email"));
      assertEquals("USER", resultSet.getString("role"));
    }
  }

  @Test
  @DisplayName("Devrait avoir l'équipe Sarah créée (vide)")
  @Transactional
  void shouldHaveSarahTeamCreated() throws Exception {
    // Given
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {

      // When - Vérifier que l'équipe Sarah existe
      ResultSet resultSet =
          statement.executeQuery(
              "SELECT t.name, t.owner_id FROM teams t "
                  + "JOIN users u ON t.owner_id = u.id "
                  + "WHERE u.username = 'Sarah'");

      // Then
      assertTrue(resultSet.next(), "L'équipe Sarah devrait exister");
      assertEquals("Équipe Sarah", resultSet.getString("name"));

      // Vérifier que l'équipe est vide (pas de joueurs)
      ResultSet playerCountResult =
          statement.executeQuery(
              "SELECT COUNT(*) as player_count FROM team_players tp "
                  + "JOIN teams t ON tp.team_id = t.id "
                  + "JOIN users u ON t.owner_id = u.id "
                  + "WHERE u.username = 'Sarah'");

      assertTrue(playerCountResult.next());
      assertEquals(0, playerCountResult.getInt("player_count"), "L'équipe Sarah devrait être vide");
    }
  }

  @Test
  @DisplayName("Devrait avoir la game 'Première Saison' créée")
  @Transactional
  void shouldHavePremiereSaisonGameCreated() throws Exception {
    // Given
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {

      // When - Vérifier que la game existe
      ResultSet resultSet =
          statement.executeQuery(
              "SELECT name, max_participants, status FROM games WHERE name = 'Première Saison'");

      // Then
      assertTrue(resultSet.next(), "La game 'Première Saison' devrait exister");
      assertEquals("Première Saison", resultSet.getString("name"));
      assertEquals(10, resultSet.getInt("max_participants"));
      assertEquals("ACTIVE", resultSet.getString("status"));
    }
  }

  @Test
  @DisplayName("Devrait avoir les règles régionales pour la Première Saison")
  @Transactional
  void shouldHaveRegionRulesForPremiereSaison() throws Exception {
    // Given
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {

      // When - Vérifier les règles régionales
      ResultSet resultSet =
          statement.executeQuery(
              "SELECT COUNT(*) as rule_count FROM game_region_rules grr "
                  + "JOIN games g ON grr.game_id = g.id "
                  + "WHERE g.name = 'Première Saison'");

      // Then
      assertTrue(resultSet.next());
      assertEquals(7, resultSet.getInt("rule_count"), "Devrait avoir 7 règles régionales");

      // Vérifier que toutes les régions ont 7 joueurs max
      ResultSet regionRules =
          statement.executeQuery(
              "SELECT region, max_players FROM game_region_rules grr "
                  + "JOIN games g ON grr.game_id = g.id "
                  + "WHERE g.name = 'Première Saison'");

      while (regionRules.next()) {
        assertEquals(
            7, regionRules.getInt("max_players"), "Chaque région devrait avoir 7 joueurs max");
      }
    }
  }

  @Test
  @DisplayName("Devrait avoir les participants pour la Première Saison")
  @Transactional
  void shouldHaveParticipantsForPremiereSaison() throws Exception {
    // Given
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {

      // When - Vérifier les participants
      ResultSet resultSet =
          statement.executeQuery(
              "SELECT COUNT(*) as participant_count FROM game_participants gp "
                  + "JOIN games g ON gp.game_id = g.id "
                  + "WHERE g.name = 'Première Saison'");

      // Then
      assertTrue(resultSet.next());
      assertEquals(3, resultSet.getInt("participant_count"), "Devrait avoir 3 participants");

      // Vérifier que Marcel, Teddy et Thibaut sont participants
      ResultSet participants =
          statement.executeQuery(
              "SELECT u.username, gp.draft_order FROM game_participants gp "
                  + "JOIN games g ON gp.game_id = g.id "
                  + "JOIN users u ON gp.user_id = u.id "
                  + "WHERE g.name = 'Première Saison' "
                  + "ORDER BY gp.draft_order");

      String[] expectedUsers = {"Thibaut", "Marcel", "Teddy"};
      int i = 0;
      while (participants.next()) {
        assertEquals(expectedUsers[i], participants.getString("username"));
        assertEquals(i + 1, participants.getInt("draft_order"));
        i++;
      }
    }
  }

  @Test
  @DisplayName("Devrait avoir les 4 utilisateurs (Marcel, Teddy, Thibaut, Sarah)")
  @Transactional
  void shouldHaveAllFourUsers() throws Exception {
    // Given
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {

      // When - Vérifier le nombre total d'utilisateurs
      ResultSet resultSet =
          statement.executeQuery("SELECT COUNT(*) as user_count FROM users WHERE role = 'USER'");

      // Then - Should have at least 4 users (may have more from other tests/services)
      assertTrue(resultSet.next());
      int userCount = resultSet.getInt("user_count");
      assertTrue(userCount >= 4, "Devrait avoir au moins 4 utilisateurs, got: " + userCount);

      // Vérifier que tous les utilisateurs attendus existent
      String[] expectedUsernames = {"Marcel", "Teddy", "Thibaut", "Sarah"};
      for (String username : expectedUsernames) {
        ResultSet userResult =
            statement.executeQuery(
                "SELECT username FROM users WHERE username = '" + username + "'");
        assertTrue(userResult.next(), "L'utilisateur " + username + " devrait exister");
      }
    }
  }
}
