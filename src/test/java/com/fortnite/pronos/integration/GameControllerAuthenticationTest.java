package com.fortnite.pronos.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
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

import com.fortnite.pronos.model.User;
import com.fortnite.pronos.repository.UserRepository;

/** Test d'intégration TDD pour vérifier l'authentification des games */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
public class GameControllerAuthenticationTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private UserRepository userRepository;

  private User testUser;

  @BeforeEach
  void setUp() {
    // Créer un utilisateur de test
    testUser = new User();
    testUser.setUsername("Thibaut");
    testUser.setEmail("thibaut@test.com");
    testUser.setPassword("password123");
    testUser = userRepository.save(testUser);
  }

  @Test
  @DisplayName("Devrait créer une game avec un utilisateur authentifié")
  void shouldCreateGameWithAuthenticatedUser() {
    // Given
    Map<String, Object> gameRequest = new HashMap<>();
    gameRequest.put("name", "Tournoi Test TDD");
    gameRequest.put("maxParticipants", 4);

    Map<String, Integer> regionRules = new HashMap<>();
    regionRules.put("EU", 2);
    regionRules.put("NAW", 1);
    regionRules.put("BR", 1);
    gameRequest.put("regionRules", regionRules);

    // When
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            "http://localhost:" + port + "/api/games?user=Thibaut", gameRequest, Map.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("name")).isEqualTo("Tournoi Test TDD");
    assertThat(response.getBody().get("creatorName")).isEqualTo("Thibaut");
  }

  @Test
  @DisplayName("Devrait rejeter la création de game sans utilisateur")
  void shouldRejectGameCreationWithoutUser() {
    // Given
    Map<String, Object> gameRequest = new HashMap<>();
    gameRequest.put("name", "Tournoi Test TDD");
    gameRequest.put("maxParticipants", 4);

    // When
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            "http://localhost:" + port + "/api/games", gameRequest, Map.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("Devrait rejeter la création de game avec un utilisateur inexistant")
  void shouldRejectGameCreationWithNonExistentUser() {
    // Given
    Map<String, Object> gameRequest = new HashMap<>();
    gameRequest.put("name", "Tournoi Test TDD");
    gameRequest.put("maxParticipants", 4);

    // When
    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            "http://localhost:" + port + "/api/games?user=UtilisateurInexistant",
            gameRequest,
            Map.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  @DisplayName("Devrait retourner les games de l'utilisateur authentifié")
  void shouldReturnUserGames() {
    // Given - Créer d'abord une game
    Map<String, Object> gameRequest = new HashMap<>();
    gameRequest.put("name", "Tournoi Test TDD");
    gameRequest.put("maxParticipants", 4);

    Map<String, Integer> regionRules = new HashMap<>();
    regionRules.put("EU", 2);
    regionRules.put("NAW", 1);
    regionRules.put("BR", 1);
    gameRequest.put("regionRules", regionRules);

    restTemplate.postForEntity(
        "http://localhost:" + port + "/api/games?user=Thibaut", gameRequest, Map.class);

    // When
    ResponseEntity<Object[]> response =
        restTemplate.getForEntity(
            "http://localhost:" + port + "/api/games?user=Thibaut", Object[].class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().length).isGreaterThan(0);
  }

  @Test
  @DisplayName("Devrait retourner un tableau vide pour un utilisateur sans games")
  void shouldReturnEmptyArrayForUserWithoutGames() {
    // When
    ResponseEntity<Object[]> response =
        restTemplate.getForEntity(
            "http://localhost:" + port + "/api/games?user=Thibaut", Object[].class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().length).isEqualTo(0);
  }
}
