package com.fortnite.pronos.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.ScoreRepository;
import com.fortnite.pronos.repository.UserRepository;

/** Test d'intégration TDD pour vérifier que la base de données H2 fonctionne correctement */
@SpringBootTest(
    classes = {
      com.fortnite.pronos.PronosApplication.class,
      com.fortnite.pronos.config.TestSecurityConfig.class
    })
@ActiveProfiles("test")
@Transactional
public class DatabaseIntegrationTest {

  @Autowired private UserRepository userRepository;

  @Autowired private PlayerRepository playerRepository;

  @Autowired private GameRepository gameRepository;

  @Autowired private ScoreRepository scoreRepository;

  @Test
  @DisplayName("Devrait avoir les utilisateurs de test dans la base de données")
  void shouldHaveFourTestUsersInDatabase() {
    // When
    List<User> users = userRepository.findAll();

    // Then
    assertThat(users).hasSizeGreaterThanOrEqualTo(4);
    assertThat(users).extracting("username").contains("Thibaut", "Marcel", "Teddy", "Sarah");
  }

  @Test
  @DisplayName("Devrait avoir les 146 joueurs Fortnite dans la base de données")
  void shouldHave147FortnitePlayersInDatabase() {
    // When
    List<Player> players = playerRepository.findAll();

    // Then
    assertThat(players).hasSize(147);

    // Vérifier que les joueurs ont des régions valides
    assertThat(players)
        .allSatisfy(
            player ->
                assertThat(player.getRegionName())
                    .isIn("EU", "NAC", "BR", "ASIA", "OCE", "NAW", "ME"));
  }

  @Test
  @DisplayName("Devrait avoir des games dans la base de données")
  void shouldHaveTestGameInDatabase() {
    // When
    List<Game> games = gameRepository.findAll();

    // Then
    assertThat(games).isNotEmpty();

    // Vérifier que les games ont des propriétés valides
    assertThat(games)
        .allSatisfy(
            game -> {
              assertThat(game.getName()).isNotNull();
              assertThat(game.getStatus()).isNotNull();
              assertThat(game.getMaxParticipants()).isGreaterThan(0);
            });
  }

  @Test
  @DisplayName("Devrait pouvoir trouver un utilisateur par nom")
  void shouldFindUserByName() {
    // When
    Optional<User> thibaut = userRepository.findByUsername("Thibaut");

    // Then
    assertThat(thibaut).isPresent();
    assertThat(thibaut.get().getUsername()).isEqualTo("Thibaut");
    assertThat(thibaut.get().getRole()).isEqualTo(User.UserRole.USER);
  }

  @Test
  @DisplayName("Devrait pouvoir trouver des joueurs par région")
  void shouldFindPlayersByRegion() {
    // When
    List<Player> euPlayers = playerRepository.findByRegion(Player.Region.EU);

    // Then
    assertThat(euPlayers).isNotEmpty();
    assertThat(euPlayers)
        .allSatisfy(player -> assertThat(player.getRegion()).isEqualTo(Player.Region.EU));
  }
}
