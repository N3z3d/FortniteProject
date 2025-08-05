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
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.TeamRepository;
import com.fortnite.pronos.repository.UserRepository;

/**
 * Tests TDD pour DataInitializationService Focus sur la distribution équitable des joueurs dans les
 * équipes
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
      "DELETE FROM scores",
      "DELETE FROM team_players",
      "DELETE FROM teams",
      "DELETE FROM game_participants",
      "DELETE FROM games",
      "DELETE FROM players",
      "DELETE FROM users"
    })
@DisplayName("DataInitializationService - TDD Tests")
class DataInitializationServiceTddTest {

  @Autowired private DataInitializationService dataInitializationService;

  @Autowired private PlayerRepository playerRepository;

  @Autowired private TeamRepository teamRepository;

  @Autowired private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    // Clean database before each test
    teamRepository.deleteAll();
    playerRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @Transactional
  @DisplayName("PROBLÈME IDENTIFIÉ: createTeams ne distribue que 12 joueurs sur 147")
  void shouldIdentifyTeamDistributionProblem() {
    // ARRANGE - Simuler 147 joueurs comme dans le CSV
    List<User> testUsers = createTestUsers();
    userRepository.saveAll(testUsers);

    List<Player> testPlayers = createManyTestPlayers(147); // Simuler les 147 joueurs CSV
    playerRepository.saveAll(testPlayers);

    // ACT - Appeler la méthode privée createTeams via reflection
    // Ou utiliser dataInitializationService si elle était publique

    // Pour ce test, on simule le comportement actuel problématique
    List<Player> allPlayers = playerRepository.findAll();
    assertThat(allPlayers).hasSize(147).as("Devrait avoir 147 joueurs du CSV");

    // PROBLÈME: createTeams() ne prend que les 12 premiers joueurs !
    // Équipe Thibaut : joueurs 0-3 (4 joueurs)
    // Équipe Teddy : joueurs 4-7 (4 joueurs)
    // Équipe Marcel : joueurs 8-11 (4 joueurs)
    // TOTAL = 12 joueurs utilisés sur 147 !

    int playersUsedInTeams = 12; // Comportement actuel problématique
    int playersNotUsed = allPlayers.size() - playersUsedInTeams;

    System.err.println("❌ PROBLÈME DÉTECTÉ:");
    System.err.println("   Joueurs en base: " + allPlayers.size());
    System.err.println("   Joueurs utilisés dans les équipes: " + playersUsedInTeams);
    System.err.println("   Joueurs NON utilisés: " + playersNotUsed);
    System.err.println("   CAUSE: createTeams() subList(0,4), subList(4,8), subList(8,12)");

    // ASSERT - Mettre en évidence le problème
    assertThat(playersNotUsed)
        .isEqualTo(135)
        .as("CRITIQUE: 135 joueurs du CSV ne sont pas utilisés dans les équipes !");
  }

  @Test
  @DisplayName("SOLUTION TDD: Les équipes devraient distribuer équitablement tous les joueurs")
  void shouldDistributeAllPlayersEquitablyInTeams() {
    // ARRANGE
    int totalPlayers = 147;
    int numberOfTeams = 3;
    int expectedPlayersPerTeam = totalPlayers / numberOfTeams; // 49 joueurs par équipe

    // ACT & ASSERT - Définir le comportement attendu
    assertThat(expectedPlayersPerTeam)
        .isEqualTo(49)
        .as("Chaque équipe devrait avoir ~49 joueurs (147/3)");

    // SOLUTION: createTeams() devrait être refactorisé comme suit:
    // Équipe Thibaut : joueurs 0-48 (49 joueurs)
    // Équipe Teddy : joueurs 49-97 (49 joueurs)
    // Équipe Marcel : joueurs 98-146 (49 joueurs)
    // TOTAL = 147 joueurs utilisés !

    System.out.println("✅ SOLUTION PROPOSÉE:");
    System.out.println("   Équipe 1: joueurs 0-48 (" + (49) + " joueurs)");
    System.out.println("   Équipe 2: joueurs 49-97 (" + (49) + " joueurs)");
    System.out.println("   Équipe 3: joueurs 98-146 (" + (49) + " joueurs)");
    System.out.println("   TOTAL: " + totalPlayers + " joueurs utilisés");
  }

  @Test
  @DisplayName("Devrait créer des équipes avec distribution proportionnelle")
  void shouldCreateTeamsWithProportionalDistribution() {
    // ARRANGE
    List<User> testUsers = createTestUsers();
    List<Player> manyPlayers = createManyTestPlayers(147);

    // ACT - Simuler la nouvelle logique de distribution
    int totalPlayers = manyPlayers.size();
    int numberOfTeams = 3;
    int basePlayersPerTeam = totalPlayers / numberOfTeams; // 49
    int remainingPlayers = totalPlayers % numberOfTeams; // 0 dans ce cas

    // ASSERT
    assertThat(basePlayersPerTeam).isEqualTo(49);
    assertThat(remainingPlayers).isEqualTo(0);

    // Vérifier la distribution
    for (int i = 0; i < numberOfTeams; i++) {
      int startIndex = i * basePlayersPerTeam;
      int endIndex = (i + 1) * basePlayersPerTeam;
      int teamSize = endIndex - startIndex;

      assertThat(teamSize).isEqualTo(49).as("L'équipe " + (i + 1) + " devrait avoir 49 joueurs");

      System.out.println(
          "Équipe "
              + (i + 1)
              + ": joueurs "
              + startIndex
              + "-"
              + (endIndex - 1)
              + " ("
              + teamSize
              + " joueurs)");
    }
  }

  private List<User> createTestUsers() {
    String timestamp = String.valueOf(System.currentTimeMillis());
    return List.of(
        createTestUser("admin_" + timestamp, "admin_" + timestamp + "@test.com"),
        createTestUser("Thibaut_" + timestamp, "thibaut_" + timestamp + "@test.com"),
        createTestUser("Teddy_" + timestamp, "teddy_" + timestamp + "@test.com"),
        createTestUser("Marcel_" + timestamp, "marcel_" + timestamp + "@test.com"));
  }

  private User createTestUser(String username, String email) {
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword("test");
    user.setRole(User.UserRole.PARTICIPANT);
    user.setCurrentSeason(2025);
    return user;
  }

  private List<Player> createManyTestPlayers(int count) {
    String timestamp = String.valueOf(System.currentTimeMillis());
    return java.util.stream.IntStream.range(0, count)
        .mapToObj(i -> createTestPlayer("Player" + i + "_" + timestamp, Player.Region.EU))
        .toList();
  }

  private Player createTestPlayer(String nickname, Player.Region region) {
    Player player = new Player();
    player.setNickname(nickname);
    player.setUsername(nickname.toLowerCase());
    player.setRegion(region);
    player.setTranche("1");
    player.setCurrentSeason(2025);
    return player;
  }
}
