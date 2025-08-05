package com.fortnite.pronos.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.model.Game;
import com.fortnite.pronos.model.Player;
import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.GameRepository;
import com.fortnite.pronos.repository.PlayerRepository;
import com.fortnite.pronos.repository.UserRepository;

/** Test d'intégration TDD pour vérifier que le frontend et le backend communiquent correctement */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@Transactional
public class FrontendBackendIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private UserRepository userRepository;

  @Autowired private PlayerRepository playerRepository;

  @Autowired private GameRepository gameRepository;

  @Test
  @DisplayName("Devrait retourner la liste des joueurs via l'API REST")
  void shouldReturnPlayersListViaRestApi() {
    // When
    ResponseEntity<Player[]> response =
        restTemplate.getForEntity("http://localhost:" + port + "/api/players", Player[].class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().length).isGreaterThan(0);

    // Vérifier que les joueurs ont des propriétés valides
    for (Player player : response.getBody()) {
      assertThat(player.getId()).isNotNull();
      assertThat(player.getUsername()).isNotNull();
      assertThat(player.getRegion()).isNotNull();
    }
  }

  @Test
  @DisplayName("Devrait retourner la liste des utilisateurs via l'API REST")
  void shouldReturnUsersListViaRestApi() {
    // When
    ResponseEntity<User[]> response =
        restTemplate.getForEntity("http://localhost:" + port + "/api/users", User[].class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().length).isGreaterThan(0);

    // Vérifier que les utilisateurs ont des propriétés valides
    for (User user : response.getBody()) {
      assertThat(user.getId()).isNotNull();
      assertThat(user.getUsername()).isNotNull();
    }
  }

  @Test
  @DisplayName("Devrait retourner la liste des games via l'API REST")
  void shouldReturnGamesListViaRestApi() {
    // When
    ResponseEntity<Game[]> response =
        restTemplate.getForEntity("http://localhost:" + port + "/api/games", Game[].class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    // Les games peuvent être vides au début
    assertThat(response.getBody().length).isGreaterThanOrEqualTo(0);
  }

  @Test
  @DisplayName("Devrait retourner un joueur spécifique via l'API REST")
  void shouldReturnSpecificPlayerViaRestApi() {
    // Given
    List<Player> players = playerRepository.findAll();
    assertThat(players).isNotEmpty();
    Player firstPlayer = players.get(0);

    // When
    ResponseEntity<Player> response =
        restTemplate.getForEntity(
            "http://localhost:" + port + "/api/players/" + firstPlayer.getId(), Player.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getId()).isEqualTo(firstPlayer.getId());
    assertThat(response.getBody().getUsername()).isEqualTo(firstPlayer.getUsername());
  }

  @Test
  @DisplayName("Devrait retourner les joueurs par région via l'API REST")
  void shouldReturnPlayersByRegionViaRestApi() {
    // When
    ResponseEntity<Player[]> response =
        restTemplate.getForEntity(
            "http://localhost:" + port + "/api/players?region=EU", Player[].class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();

    // Vérifier que tous les joueurs retournés sont de la région EU
    for (Player player : response.getBody()) {
      assertThat(player.getRegion()).isEqualTo(Player.Region.EU);
    }
  }
}
